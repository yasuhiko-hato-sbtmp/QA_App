package jp.techacademy.yasuhiko.hato.qa_app;



import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;


public class MainActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private int mGenre = 0;

    private DatabaseReference mDatabaseReference;
    private DatabaseReference mGenreRef;
    private ListView mListView;
    private ArrayList<Question> mQuestionArrayList;
    private QuestionsListAdapter mAdapter;

    private ChildEventListener mEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            if(mGenre != 0) {
                HashMap map = (HashMap) dataSnapshot.getValue();
                Question question = getQuestionFromHashMap(map, dataSnapshot.getKey());
                mQuestionArrayList.add(question);
                mAdapter.notifyDataSetChanged();
            }
            else{
                HashMap maps = (HashMap) dataSnapshot.getValue();
                Set<String> keys = maps.keySet();
                for(String key: keys){
                    HashMap map = (HashMap) maps.get(key);
                    Question question = getQuestionFromHashMap(map, key);
                    if(question.getFav().equals("true")){
                        mQuestionArrayList.add(question);
                    }
                }
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();

            if(mGenre != 0) {
                // 変更があったQuestionを探す
                for (Question question : mQuestionArrayList) {
                    if (dataSnapshot.getKey().equals(question.getQuestionUid())) {
                        // for Answers
                        question.getAnswers().clear();
                        HashMap answerMap = (HashMap) map.get("answers");
                        if (answerMap != null) {
                            for (Object key : answerMap.keySet()) {
                                HashMap temp = (HashMap) answerMap.get((String) key);
                                String answerBody = (String) temp.get("body");
                                String answerName = (String) temp.get("name");
                                String answerUid = (String) temp.get("uid");
                                Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);
                                question.getAnswers().add(answer);
                            }
                        }

                        mAdapter.notifyDataSetChanged();
                    }
                }
            }
            else{
                Set<String> keys = map.keySet();
                for(String key: keys) { // for all genres
                    HashMap tmpMap = (HashMap) map.get(key);
                    Question tmpQuestion = getQuestionFromHashMap(tmpMap, key);
                    int i = 0;
                    for (Question question : mQuestionArrayList) {
                        if (question.getQuestionUid().equals(tmpQuestion.getQuestionUid())) {
                            // for Fav
                            if(tmpQuestion.getFav().equals("false")){
                                mQuestionArrayList.remove(i);
                            }
                            // For answers
                            if(tmpQuestion.getAnswers().size() > question.getAnswers().size()){
                                question.getAnswers().clear();
                                HashMap answerMap = (HashMap) tmpMap.get("answers");
                                if (answerMap != null) {
                                    for (Object tmpKey : answerMap.keySet()) {
                                        HashMap temp = (HashMap) answerMap.get((String) tmpKey);
                                        String answerBody = (String) temp.get("body");
                                        String answerName = (String) temp.get("name");
                                        String answerUid = (String) temp.get("uid");
                                        Answer answer = new Answer(answerBody, answerName, answerUid, (String) tmpKey);
                                        question.getAnswers().add(answer);
                                    }
                                }
                            }
                            mAdapter.notifyDataSetChanged();
                        }
                        i++;
                    }
                }
            }

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };


    private Question getQuestionFromHashMap(HashMap map, String qId){
        String title = (String) map.get("title");
        String body = (String) map.get("body");
        String name = (String) map.get("name");
        String uid = (String) map.get("uid");
        String imageString = (String) map.get("image");
        Bitmap image = null;
        byte[] bytes;
        if (imageString != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            bytes = Base64.decode(imageString, Base64.DEFAULT);
        } else {
            bytes = new byte[0];
        }
        String fav = (String) map.get("fav");

        ArrayList<Answer> answerArrayList = new ArrayList<Answer>();
        HashMap answerMap = (HashMap) map.get("answers");
        if (answerMap != null) {
            for (Object key : answerMap.keySet()) {
                HashMap temp = (HashMap) answerMap.get((String) key);
                String answerBody = (String) temp.get("body");
                String answerName = (String) temp.get("name");
                String answerUid = (String) temp.get("uid");
                Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);
                answerArrayList.add(answer);
            }
        }
        int genre = Integer.parseInt((String) map.get("genre"));

        Question question = new Question(title, body, name, uid, qId, genre, bytes, answerArrayList, fav);
        return question;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ジャンルを選択していない場合（mGenre == 0）はエラーを表示するだけ
                if (mGenre == 0) {
                    Snackbar.make(view, "ジャンルを選択して下さい", Snackbar.LENGTH_LONG).show();
                    return;
                }

                // ログイン済みのユーザーを収録する
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                if (user == null) {
                    // ログインしていなければログイン画面に遷移させる
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                } else {
                    // ジャンルを渡して質問作成画面を起動する
                    Intent intent = new Intent(getApplicationContext(), QuestionSendActivity.class);
                    intent.putExtra("genre", mGenre);
                    startActivity(intent);
                }

            }
        });

        // ナビゲーションドロワーの設定
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, mToolbar, R.string.app_name, R.string.app_name);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_hobby) {
                    mToolbar.setTitle("趣味");
                    mGenre = 1;
                } else if (id == R.id.nav_life) {
                    mToolbar.setTitle("生活");
                    mGenre = 2;
                } else if (id == R.id.nav_health) {
                    mToolbar.setTitle("健康");
                    mGenre = 3;
                } else if (id == R.id.nav_compter) {
                    mToolbar.setTitle("コンピューター");
                    mGenre = 4;
                }
                else if(id == R.id.nav_fav){ // お気に入り
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        // ログインしていなければログイン画面に遷移させる
                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        startActivity(intent);
                    } else {
                        mToolbar.setTitle("お気に入り");
                        mGenre = 0;
                    }

                }

                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                drawer.closeDrawer(GravityCompat.START);

                // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
                mQuestionArrayList.clear();
                mAdapter.setQuestionArrayList(mQuestionArrayList);
                mListView.setAdapter(mAdapter);

                // 選択したジャンルにリスナーを登録する
                if (mGenreRef != null) {
                    mGenreRef.removeEventListener(mEventListener);
                }
                if(mGenre != 0) {
                    mGenreRef = mDatabaseReference.child(Const.ContentsPATH).child(String.valueOf(mGenre));
                }
                else{
                    mGenreRef = mDatabaseReference.child(Const.ContentsPATH);
                }
                mGenreRef.addChildEventListener(mEventListener);
                return true;
            }
        });

        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        // ListViewの準備
        mListView = (ListView) findViewById(R.id.listView);
        mAdapter = new QuestionsListAdapter(this);
        mQuestionArrayList = new ArrayList<Question>();
        mAdapter.notifyDataSetChanged();

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Questionのインスタンスを渡して質問詳細画面を起動する
                Intent intent = new Intent(getApplicationContext(), QuestionDetailActivity.class);
                intent.putExtra("question", mQuestionArrayList.get(position));
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(getApplicationContext(), SettingActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
