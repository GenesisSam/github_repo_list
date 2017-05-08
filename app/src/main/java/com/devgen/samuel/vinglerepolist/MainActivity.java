package com.devgen.samuel.vinglerepolist;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.devgen.samuel.vinglerepolist.dataset.repo;
import com.devgen.samuel.vinglerepolist.dataset.userInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cz.msebera.android.httpclient.Header;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "VingleRepoList";
    private String userName = "";

    private RecyclerView listview;
    private LinearLayoutManager llm;
    private RecyclerAdapter radapter;

    private ArrayList<repo> repoArrayList = new ArrayList<>();
    private userInfo user;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get incomming data
        if(Intent.ACTION_VIEW.equals(getIntent().getAction())){
            Uri data = getIntent().getData();
            userName = getUsernameFromURL(data.toString());
        }

        if (!userName.isEmpty()) {
            fetchData();
        }
    }

    private void initViews() {
        listview = (RecyclerView)findViewById(R.id.listview);
        listview.addItemDecoration(new myRecyclerViewDecorator(10));
        llm = new LinearLayoutManager(MainActivity.this);
        listview.setHasFixedSize(true);
        listview.setLayoutManager(llm);
        radapter = new RecyclerAdapter(MainActivity.this, repoArrayList, user);
        listview.setAdapter(radapter);
    }

    private String getUsernameFromURL(String data) {
        data = data.replaceFirst("testapp://", "");
        String[] datas = data.split("/");
        if(datas.length > 1)
            return datas[1];
        else
            return "";
    }

    private Comparator myComparator = new Comparator<repo>() {
        @Override
        public int compare(repo obj1, repo obj2) {
            return obj2.getStargazers_count().compareTo(obj1.getStargazers_count());
        }
    };

    private void fetchData() {
        String userUrl = "https://api.github.com/users/"+userName.trim();
        final String reposUrl = userUrl+"/repos";

        String ua = new WebView(this).getSettings().getUserAgentString();

        AsyncHttpClient asyncUserInfo = new AsyncHttpClient();
        asyncUserInfo.setUserAgent(ua);
        final AsyncHttpClient asyncRepos = new AsyncHttpClient();
        asyncRepos.setUserAgent(ua);

        final ProgressDialog pd = new ProgressDialog(MainActivity.this);
        pd.setMessage("Fetching user information...");
        pd.setIndeterminate(false);
        pd.setCancelable(false);
        pd.show();

        asyncUserInfo.get(MainActivity.this, userUrl, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                pd.setMessage("Fetching repositories...");
                asyncRepos.get(MainActivity.this, reposUrl, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        String result = new String(responseBody);
                        Gson g = new Gson();
                        repoArrayList = g.fromJson(result, new TypeToken<List<repo>>(){}.getType());
                        Collections.sort(repoArrayList, myComparator);

                        pd.dismiss();
                        Toast.makeText(MainActivity.this, "Done", Toast.LENGTH_SHORT).show();

                        initViews();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        pd.dismiss();
                        String m = new String(responseBody);
                        Log.e(TAG, m);
                        Toast.makeText(MainActivity.this, "Failed to fetching repositories.", Toast.LENGTH_SHORT).show();
                    }
                });

                String result = new String(responseBody);
                Gson g = new Gson();
                user = g.fromJson(result, userInfo.class);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                pd.dismiss();
                String m = new String(responseBody);
                Log.e(TAG, m);
                Toast.makeText(MainActivity.this, "Failed to fetching user information."+m, Toast.LENGTH_SHORT).show();
            }
        });

    }

    private class RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
        private Context mContext;
        private ArrayList<repo> repos;
        private userInfo userInfo;

        public RecyclerAdapter(Context mContext, ArrayList<repo> repos, userInfo userInfo) {
            this.mContext = mContext;
            this.repos = repos;
            this.userInfo = userInfo;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return 0;
            } else {
                return 1;
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v;
            switch (viewType) {
                case 0:
                    v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_useinfo, null);
                    return new UserInfoViewHolder(v);
                case 1:
                    v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_repo, null);
                    return new RepoViewHolder(v);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0:{
                    UserInfoViewHolder viewHolder = (UserInfoViewHolder)holder;
                    viewHolder.tvName.setText(userInfo.getName());
                    viewHolder.setUserImage(userInfo.getAvatar_url());
                }
                break;
                case 1:{
                    repo data = this.repos.get(position - 1);
                    RepoViewHolder viewHolder = (RepoViewHolder)holder;
                    viewHolder.tvName.setText(data.getName());
                    viewHolder.tvDesc.setText(data.getDescription());
                    viewHolder.tvStart.setText(""+data.getStargazers_count());
                }
                break;
            }
        }

        @Override
        public int getItemCount() {
            if (userInfo != null) {
                return repos.size() + 1;
            } else {
                return repos.size();
            }
        }

        public class RepoViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDesc, tvStart;

            public RepoViewHolder(View itemView){
                super(itemView);
                this.tvName = (TextView)itemView.findViewById(R.id.txtRepoName);
                this.tvDesc = (TextView)itemView.findViewById(R.id.txtRepoDesc);
                this.tvStart = (TextView)itemView.findViewById(R.id.txtStar);
            }
        }

        public class UserInfoViewHolder extends RecyclerView.ViewHolder implements loadImageFromURL.Listener{
            TextView tvName;
            ImageView imgProfile;

            public UserInfoViewHolder(View itemView){
                super(itemView);
                this.tvName = (TextView)itemView.findViewById(R.id.txtUsername);
                this.imgProfile = (ImageView)itemView.findViewById(R.id.imgUserProfile);
            }

            public void setUserImage(String url) {
                new loadImageFromURL(this).execute(url);
            }

            @Override
            public void onImageLoaded(Bitmap bitmap) {
                this.imgProfile.setImageBitmap(bitmap);
            }

            @Override
            public void onError() {
                Log.e(TAG, "Failed to load image and convert to bitmap");
            }
        }
    }

    private class myRecyclerViewDecorator extends RecyclerView.ItemDecoration {
        private final int divHeight;

        public myRecyclerViewDecorator(int divHeight) {
            this.divHeight = divHeight;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            outRect.top = divHeight;
        }
    }
}
