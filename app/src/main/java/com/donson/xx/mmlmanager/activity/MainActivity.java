package com.donson.xx.mmlmanager.activity;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.donson.xx.mmlmanager.MLManagerApplication;
import com.donson.xx.mmlmanager.R;
import com.donson.xx.mmlmanager.adapter.AppAdapter;
import com.donson.xx.mmlmanager.beans.AppInfo;
import com.donson.xx.mmlmanager.util.AppPreferences;
import com.donson.xx.mmlmanager.util.UtilsApp;
import com.donson.xx.mmlmanager.util.UtilsUI;
import com.donson.xx.mmlmanager.widget.ProgressWhell;
import com.donson.xx.mmlmanager.widget.PullToRefreshView;
import com.mikepenz.materialdrawer.Drawer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;


public class MainActivity extends AppCompatActivity
        implements /*NavigationView.OnNavigationItemSelectedListener ,*/ SearchView.OnQueryTextListener {

    private AppPreferences appPreferences;

    private List<AppInfo> appList;
    private List<AppInfo> appSystemList;
    private List<AppInfo> appHiddenList;
    private AppAdapter appAdapter;
    private AppAdapter appSystemAdapter;
    private AppAdapter appFavoriteAdapter;
    private AppAdapter appHiddenAdapter;
    private Boolean aoubleBackToExitPressOnce = false;
    private Toolbar toolbar;
    private Activity activity;
    private Context context;

    private RecyclerView recyclerView;
    private PullToRefreshView pullToRefreshView;
    private ProgressWhell progressWhell;
    private Drawer drawer;

    private MenuItem searchItem;
    private SearchView searchView;
    private static LinearLayout noResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.appPreferences = MLManagerApplication.getAppPreferences();
        this.activity = this;
        this.context = this;
        setInitialConfiguration();
        recyclerView = (RecyclerView) findViewById(R.id.appList);
        pullToRefreshView = (PullToRefreshView) findViewById(R.id.pull_to_refresh);
        noResults = (LinearLayout) findViewById(R.id.noRseults);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        drawer = UtilsUI.setNavigationDrqwer((Activity)context,context,toolbar,
                appAdapter,appSystemAdapter,appFavoriteAdapter,appHiddenAdapter,recyclerView);
        new getInstalledApps().execute();


    }

    private void setInitialConfiguration() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("MM Manager");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(UtilsUI.darker(appPreferences.getPrimaryColorPref(), 0.8));
            ;
//            toolbar.setBackgroundColor();
            getWindow().setNavigationBarColor(getResources().getColor(R.color.colorAccent));

        }
    }

    public static void setResultsMessage(boolean result) {
        if(result){
            noResults.setVisibility(View.VISIBLE);
        }else {
            noResults.setVisibility(View.GONE);
        }
    }

    class getInstalledApps extends AsyncTask<Void, String, Void> {
        private Integer totalApps;
        private Integer actualApps;

        public getInstalledApps() {
            actualApps = 0;
            appList = new ArrayList<>();
            appSystemList = new ArrayList<>();
            appHiddenList = new ArrayList<>();
        }

        @Override
        protected Void doInBackground(Void... params) {
            final PackageManager packageManager = getPackageManager();
            List<PackageInfo> packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA);
            Set<String> hiddenApps = appPreferences.getHiddenApps();
            PackageInfo p1 = packages.get(0);
            CharSequence lable = packageManager.getApplicationLabel(p1.applicationInfo);
            Long size1 = new File(p1.applicationInfo.sourceDir).length();
            System.out.println("lable::" + lable + " size1:" + size1);
            totalApps = packages.size() + hiddenApps.size();
            switch (appPreferences.getSortMode()) {
                default:
                    Collections.sort(packages, new Comparator<PackageInfo>() {
                        @Override
                        public int compare(PackageInfo p1, PackageInfo p2) {
                            return packageManager.getApplicationLabel(p1.applicationInfo).toString().toLowerCase().compareTo(packageManager.getApplicationLabel(p2.applicationInfo).toString().toLowerCase());
                        }
                    });
                    break;
                case "2":
                    Collections.sort(packages, new Comparator<PackageInfo>() {
                        @Override
                        public int compare(PackageInfo p1, PackageInfo p2) {
                            Long size1 = new File(p1.applicationInfo.sourceDir).length();
                            Long size2 = new File(p2.applicationInfo.sourceDir).length();
                            return size2.compareTo(size1);
                        }
                    });
                    break;
                case "3":
                    Collections.sort(packages, new Comparator<PackageInfo>() {
                        @Override
                        public int compare(PackageInfo p1, PackageInfo p2) {
                            return Long.toString(p2.firstInstallTime).compareTo(Long.toString(p1.firstInstallTime));
                        }
                    });
                    break;
                case "4":
                    Collections.sort(packages, new Comparator<PackageInfo>() {
                        @Override
                        public int compare(PackageInfo p1, PackageInfo p2) {
                            return Long.toString(p2.lastUpdateTime).compareTo(Long.toString(p1.lastUpdateTime));
                        }
                    });
                    break;
            }
            for (PackageInfo packageInfo : packages) {
                if (!(packageManager.getApplicationLabel(packageInfo.applicationInfo).equals("") || packageInfo.packageName.equals(""))) {
                    if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                        try {
                            AppInfo tempApp = new AppInfo(packageManager.getApplicationLabel(packageInfo.applicationInfo).toString(),
                                    packageInfo.packageName, packageInfo.versionName, packageInfo.applicationInfo.sourceDir,
                                    packageInfo.applicationInfo.dataDir,
                                    packageManager.getApplicationIcon(packageInfo.applicationInfo),
                                    false);
                            appList.add(tempApp);
                        } catch (OutOfMemoryError e) {
                            AppInfo tempApp = new AppInfo(packageManager.getApplicationLabel(packageInfo.applicationInfo).toString(),
                                    packageInfo.packageName,
                                    packageInfo.versionName,
                                    packageInfo.applicationInfo.sourceDir,
                                    packageInfo.applicationInfo.dataDir,
                                    getResources().getDrawable(R.drawable.ic_android),
                                    false);
                            appList.add(tempApp);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {//系统应用
                        try {
                            AppInfo tempApp = new AppInfo(packageManager.getApplicationLabel(packageInfo.applicationInfo).toString(),
                                    packageInfo.packageName,
                                    packageInfo.versionName,
                                    packageInfo.applicationInfo.sourceDir,
                                    packageInfo.applicationInfo.dataDir,
                                    packageManager.getApplicationIcon(packageInfo.applicationInfo),
                                    true);
                            appSystemList.add(tempApp);
                        } catch (OutOfMemoryError e) {

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                actualApps++;
                publishProgress(Double.toString((actualApps * 100) / totalApps));
            }
            for (String app:hiddenApps){

                AppInfo tempApp = new AppInfo(app);
                Drawable tempAppIcon = UtilsApp.getIconFromCache(context,tempApp);
                tempApp.setIcon(tempAppIcon);
                appHiddenList.add(tempApp);
                actualApps++;
                publishProgress(Double.toString((actualApps*100)/totalApps));
            }

            return null;
        }
//        public static void setResultsMessage(Boolean result) {
//            if (result) {
//                noResults.setVisibility(View.VISIBLE);
//                fastScroller.setVisibility(View.GONE);
//            } else {
//                noResults.setVisibility(View.GONE);
//                fastScroller.setVisibility(View.VISIBLE);
//            }
//        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            appAdapter = new AppAdapter(appList,context);
            appSystemAdapter = new AppAdapter(appSystemList, context);
//            appFavoriteAdapter = new AppAdapter(getFavoriteList(appList, appSystemList), context);
            appHiddenAdapter = new AppAdapter(appHiddenList, context);
            recyclerView.setAdapter(appAdapter);
            setPullToRefershView(pullToRefreshView);
            searchItem.setVisible(true);

        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
//            progressWhell.setProgress(Float.parseFloat(values[0]));
        }
    }

    private void setPullToRefershView(final PullToRefreshView pullToRefreshView) {
        pullToRefreshView.setOnRefreshListener(new PullToRefreshView.OnRefreshListener() {
            @Override
            public void onRefresh() {
                appAdapter.clear();
                recyclerView.setAdapter(null);
                new getInstalledApps().execute();
                pullToRefreshView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pullToRefreshView.setRefreshing(false);
                    }
                },1000);

            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);
        SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if(newText.isEmpty()){
            ((AppAdapter)recyclerView.getAdapter()).getFilter().filter("");
        }else {
            ((AppAdapter)recyclerView.getAdapter()).getFilter().filter(newText.toLowerCase());
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //    @SuppressWarnings("StatementWithEmptyBody")
//    @Override
//    public boolean onNavigationItemSelected(MenuItem item) {
//         Handle navigation view item clicks here.
//        int id = item.getItemId();
//
//        if (id == R.id.nav_camera) {
//            // Handle the camera action
//        } else if (id == R.id.nav_gallery) {
//
//        } else if (id == R.id.nav_slideshow) {
//
//        } else if (id == R.id.nav_manage) {
//
//        } else if (id == R.id.nav_share) {
//
//        } else if (id == R.id.nav_send) {
//
//        }
//
//        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
//        drawer.closeDrawer(GravityCompat.START);
//        return true;
//    }
//
    @Override
    public void onBackPressed() {
//        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
//        if (drawer.isDrawerOpen(GravityCompat.START)) {
//            drawer.closeDrawer(GravityCompat.START);
//        } else {
//            super.onBackPressed();
//        }
    }
}
