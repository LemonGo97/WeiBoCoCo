package top.elizabath.weibococo.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import android.view.KeyEvent;
import android.widget.Toast;
import com.dueeeke.videoplayer.player.IjkVideoView;
import com.dueeeke.videoplayer.player.VideoViewManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;

import android.view.MenuItem;

import com.google.android.material.navigation.NavigationView;
import com.scwang.smartrefresh.layout.api.RefreshLayout;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Menu;
import android.widget.RelativeLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;
import okhttp3.Call;

import okhttp3.Response;
import top.elizabath.weibococo.R;
import top.elizabath.weibococo.ui.adapter.WeiBoAdapter;
import top.elizabath.weibococo.ui.entity.WeiBoBean;
import top.elizabath.weibococo.ui.entity.WeiBoSearchResult;
import top.elizabath.weibococo.ui.util.*;
import top.elizabath.weibococo.ui.view.SearchBarEditText;

public class HomeActivity extends ActivityBase
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {
    private final String TAG = getClass().getSimpleName();
    private final int UPDATE_WEIBO_LIST_SUCCESS = 200;
    private final int UPDATE_WEIBO_LIST_FAILED = 500;
    private int nowPage = 1;

    private SearchBarEditText search;
    private RelativeLayout search_bar;
    private DrawerLayout drawer;
    private Toolbar toolbar;
    private NavigationView navigationView;
    private FloatingActionButton fab;
    private RecyclerView weibolist;
    private RefreshLayout swipeRefresh;
    private WeiBoAdapter weiBoAdapter;
    private List<WeiBoBean> weiBoList = new ArrayList<>();

    private boolean searchFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initView();
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1);
        swipeRefresh.autoRefreshAnimationOnly();
        searchPopularWeiBo(nowPage, false);
        weibolist.setLayoutManager(layoutManager);
        weiBoAdapter = new WeiBoAdapter(weiBoList);
        weibolist.setAdapter(weiBoAdapter);
        weibolist.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {

            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {
                IjkVideoView ijkVideoView = view.findViewById(R.id.weiboVideo);
                if (ijkVideoView != null && !ijkVideoView.isFullScreen()) {
                    ijkVideoView.stopPlayback();
                }
            }
        });
        setSupportActionBar(toolbar);
        fab.setOnClickListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        search.setOnDrawableLeftListener(() -> {
            //左侧Drawable点击监听
            search_bar.setVisibility(View.GONE);
            search.setText("");
            searchFlag = false;
            nowPage = 1;
            swipeRefresh.finishRefresh(false);
            swipeRefresh.finishLoadMore(false);
            ToastUtil.showToast(HomeActivity.this, "取消搜索");
        });
        search.setOnDrawableRightListener(() -> {
            //右侧Drawable点击监听
            nowPage = 1;
            swipeRefresh.autoRefreshAnimationOnly();
            searchRealTimeWeiBo(search.getText().toString(), nowPage, false);
        });
        swipeRefresh.setOnRefreshListener(refreshlayout -> {
            refreshWeiBoLists();
        });
        swipeRefresh.setOnLoadMoreListener(loadMoreLayout -> {
            loadMoreWeiBoLists();
        });
    }

    @Override
    public void onBackPressed() {
        if (!VideoViewManager.instance().onBackPressed()){
            super.onBackPressed();
        }
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private long exitTime = 0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (swipeRefresh != null) {
            swipeRefresh.finishRefresh();
            swipeRefresh.finishLoadMore();
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void exit() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(getApplicationContext(),
                    "再按一次退出程序", Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            finish();
            System.exit(0);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            search_bar.setVisibility(View.VISIBLE);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // Handle the camera action
            if (!"HomeActivity".equals(getClass().getSimpleName())) {
                Intent intent = new Intent(ApplicationManager.getContext(),HomeActivity.class);
                startActivity(intent);
            }
        } else if (id == R.id.nav_report) {
            Intent intent = new Intent(ApplicationManager.getContext(),ReportMsgActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_tools) {
            ToastUtil.showToast(ApplicationManager.getContext(),"此功能还未开发");
        } else if (id == R.id.nav_share) {
            ToastUtil.showToast(ApplicationManager.getContext(),"此功能还未开发");
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab:
                swipeRefresh.autoRefresh();
                break;
            default:
                break;
        }
    }

    public void searchRealTimeWeiBo(String searchKey, int pageNumber, boolean isLoadMore) {
        if (pageNumber < 1) {
            ToastUtil.showToast(HomeActivity.this, "没有更多了哦");
        }
        if (null == searchKey || "".equals(searchKey.trim())) {
            return;
        }
        searchKey = URLHelper.encode(searchKey);
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("containerid", "100103type%3D61%26q%3D" + searchKey + "%26t%3D0");
        params.put("page_type", "searchall");
        params.put("page", String.valueOf(pageNumber));
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Mobile Safari/537.36");
        getWeiBoList(params, headers, "search", isLoadMore);
        searchFlag = true;
    }

    public void searchPopularWeiBo(int pageNumber, boolean isLoadMore) {
        if (pageNumber < 1) {
            ToastUtil.showToast(HomeActivity.this, "没有更多了哦");
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("containerid", "102803");
        params.put("openApp", "0");
        params.put("since_id", String.valueOf(pageNumber));
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        headers.put("Host", "m.weibo.cn");
        headers.put("Connection", "keep-alive");
        headers.put("Cache-Control", "max-age=0");
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
        headers.put("Upgrade-Insecure-Requests", "1");
        headers.put("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.put("Cookie", "_T_WM=16787142025; WEIBOCN_FROM=1110006030; MLOGIN=0; M_WEIBOCN_PARAMS=luicode%3D10000011%26lfid%3D102803%26fid%3D102803%26uicode%3D10000011");
        getWeiBoList(params, headers, "popular", isLoadMore);
    }

    public void getWeiBoList(LinkedHashMap<String, String> params, LinkedHashMap<String, String> headers, String way, boolean isLoadMore) {
        OkHttpUtils.get().url(KEYManage.WEIBO_SEARCH_URL).params(params).headers(headers).build().execute(new StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {
                // 异常处理
                Log.d(TAG, "请求失败");
                Message message = new Message();
                message.what = UPDATE_WEIBO_LIST_FAILED;
                handler.sendMessage(message);
            }

            @Override
            public void onResponse(String responseData, int id) {
                // 返回数据
                if (!isLoadMore) {
                    weiBoList.clear();
                }
                if ("popular".equals(way)) {
                    WeiBoSearchResult result = GsonUtil.fromJson(responseData, WeiBoSearchResult.class);
                    List<WeiBoBean> weiBoBeans = result.getData().getCards();
                    weiBoList.addAll(weiBoBeans);
                } else if ("search".equals(way)) {
                    WeiBoSearchResult result = GsonUtil.fromJson(responseData, WeiBoSearchResult.class);
                    try {
                        List<WeiBoBean> weiBoBeans = result.getData().getCards().get(0).getCard_group();
                        weiBoList.addAll(weiBoBeans);
                    } catch (Exception e) {
                        Message message = new Message();
                        message.what = UPDATE_WEIBO_LIST_FAILED;
                        handler.sendMessage(message);
                        Log.e(TAG, "onResponse: ", e);
                        return;
                    }
                    if (null == weiBoList) {
                        weiBoList = new ArrayList<>();
                    }
                }
                Message message = new Message();
                message.what = UPDATE_WEIBO_LIST_SUCCESS;
                handler.sendMessage(message);
            }
        });
    }

    private void initView() {
        search = findViewById(R.id.search_edit_text);
        search_bar = findViewById(R.id.search_layout);
        toolbar = findViewById(R.id.toolbar);
        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        fab = findViewById(R.id.fab);
        weibolist = findViewById(R.id.weibolist);
        swipeRefresh = findViewById(R.id.weibolistRefresh);
    }

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case UPDATE_WEIBO_LIST_SUCCESS:
                    runOnUiThread(() -> {
                        swipeRefresh.finishRefresh();
                        swipeRefresh.finishLoadMore();
                        swipeRefresh.closeHeaderOrFooter();
                    });
                    weiBoAdapter.notifyDataSetChanged();
                    break;
                case UPDATE_WEIBO_LIST_FAILED:
                    runOnUiThread(() -> {
                        swipeRefresh.finishRefresh(false);
                        swipeRefresh.finishLoadMore(false);
                        swipeRefresh.closeHeaderOrFooter();
                    });
                    break;
                default:
                    break;
            }
        }
    };


    private void refreshWeiBoLists() {
        nowPage = 1;
        if (searchFlag) {
            searchRealTimeWeiBo(search.getText().toString(), nowPage, false);
        } else {
            searchPopularWeiBo(nowPage, false);
        }
    }

    private void loadMoreWeiBoLists() {
        nowPage += 1;
        if (searchFlag) {
            searchRealTimeWeiBo(search.getText().toString(), nowPage, true);
        } else {
            searchPopularWeiBo(nowPage, true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        VideoViewManager.instance().releaseVideoPlayer();
    }
}
