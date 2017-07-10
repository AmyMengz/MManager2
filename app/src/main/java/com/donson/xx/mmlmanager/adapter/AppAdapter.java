package com.donson.xx.mmlmanager.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.donson.xx.mmlmanager.MLManagerApplication;
import com.donson.xx.mmlmanager.R;
import com.donson.xx.mmlmanager.activity.AppActivity;
import com.donson.xx.mmlmanager.activity.MainActivity;
import com.donson.xx.mmlmanager.beans.AppInfo;
import com.donson.xx.mmlmanager.util.AppPreferences;
import com.donson.xx.mmlmanager.util.UtilsApp;
import com.donson.xx.mmlmanager.util.UtilsDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/4/27.
 */

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> implements Filterable {
    // Load Settings
    private AppPreferences appPreferences;

    // AppAdapter variables
    private List<AppInfo> appList;
    private List<AppInfo> appListSearch;
    private Context context;
    public AppAdapter(List<AppInfo> appList, Context context){
        this.appList = appList;
        this.context = context;
        this.appPreferences = MLManagerApplication.getAppPreferences();
    }

    @Override
    public AppViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View appAdapterView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.app_layout, viewGroup, false);
        return new AppViewHolder(appAdapterView);
    }


    @Override
    public int getItemCount() {
        return appList.size();
    }

    public void clear(){
        appList.clear();
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(AppViewHolder holder, int position) {
        AppInfo appInfo = appList.get(position);
        holder.vName.setText(appInfo.getName());
        holder.vApk.setText(appInfo.getAPK());
        holder.vIcon.setImageDrawable(appInfo.getIcon());

        setButtonEvents(holder, appInfo);

    }

    private void setButtonEvents(AppViewHolder holder, final AppInfo appInfo) {
        TextView appExtract = holder.vExtract;
        TextView appShare = holder.vShare;
        final ImageView appIcon = holder.vIcon;
        final CardView cardView = holder.vCard;
        appExtract.setBackgroundColor(appPreferences.getPrimaryColorPref());
        appShare.setBackgroundColor(appPreferences.getPrimaryColorPref());
        appExtract.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialDialog dialog = UtilsDialog.showTitleContentWithProgress(context
                        , String.format(context.getResources().getString(R.string.dialog_saving), appInfo.getName())
                        , context.getResources().getString(R.string.dialog_saving_description));
                new ExtractFileInBackground(context, dialog, appInfo).execute();

            }
        });
        appShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UtilsApp.copyFile(appInfo);
                Intent shareIntent = UtilsApp.getShareIntent(UtilsApp.getOutputFilename(appInfo));
                context.startActivity(Intent.createChooser(shareIntent,String.format("Send %s using",appInfo.getName())));
            }
        });
        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity activity = (Activity)context;
                Intent intent = new Intent(context, AppActivity.class);
                intent.putExtra("app_name", appInfo.getName());
                intent.putExtra("app_apk", appInfo.getAPK());
                intent.putExtra("app_version", appInfo.getVersion());
                intent.putExtra("app_source", appInfo.getSource());
                intent.putExtra("app_data", appInfo.getData());
                Bitmap bitmap = ((BitmapDrawable) appInfo.getIcon()).getBitmap();
                intent.putExtra("app_icon", bitmap);
                intent.putExtra("app_isSystem", appInfo.isSystem());

//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                    String transitionName = context.getResources().getString(R.string.transition_app_icon);
//
//                    ActivityOptions transitionActivityOptions = ActivityOptions.makeSceneTransitionAnimation(activity, appIcon, transitionName);
//                    context.startActivity(intent, transitionActivityOptions.toBundle());
//                } else {
                    context.startActivity(intent);
//                    activity.overridePendingTransition(R.anim.slide_in_right, R.anim.fade_back);
//                }
            }
        });
    }
    public  class ExtractFileInBackground  extends AsyncTask<Void,String,Boolean>{
        private Context context;
        private Activity activity;
        private MaterialDialog dialog;
        private AppInfo appInfo;
        public ExtractFileInBackground(Context context,MaterialDialog dialog,AppInfo appInfo){
            this.activity = (Activity)context;
            this.context = context;
            this.dialog = dialog;
            this.appInfo = appInfo;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Boolean status = false;
            if(UtilsApp.checkPermissions(activity)){
                if(!appInfo.getAPK().equals(MLManagerApplication.getProPackage())){
                    status = UtilsApp.copyFile(appInfo);
                }else {
                    status = UtilsApp.extractMLManagerPro(context,appInfo);
                }
            }
            return status;
        }

        @Override
        protected void onPostExecute(Boolean status) {
            super.onPostExecute(status);
            dialog.dismiss();
            if(status){
//                UtilsDialog.showSnackbar(activity,String.format(context.getResources().getString(R.string.dialog_saved_description), appInfo.getName(), UtilsApp.getAPKFilename(appInfo)), context.getResources().getString(R.string.button_undo), UtilsApp.getOutputFilename(appInfo), 1).show();
            }else {
                UtilsDialog.showTitleContent(context, context.getResources().getString(R.string.dialog_extract_fail), context.getResources().getString(R.string.dialog_extract_fail_description));
            }
        }
    }

    @Override
    public Filter getFilter() {
        return new Filter(){

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                final FilterResults oReturn = new FilterResults();
                final List<AppInfo> results = new ArrayList<>();
                if(appListSearch == null){
                    appListSearch = appList;
                }
                if (constraint != null){
                    if(appListSearch!=null&&appListSearch.size()>0){
                        for (final AppInfo appInfo:appListSearch){
                            if(appInfo.getName().toLowerCase().contains(constraint.toString())){
                                results.add(appInfo);
                            }
                        }
                    }
                    oReturn.values = results;
                    oReturn.count = results.size();
                }
                return oReturn;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                MainActivity.setResultsMessage(!(results.count>0));

                appList = (ArrayList<AppInfo>) results.values;
                notifyDataSetChanged();

            }
        };
    }

    public class AppViewHolder extends RecyclerView.ViewHolder {
        protected TextView vName;
        protected TextView vApk;
        protected ImageView vIcon;
        protected TextView vExtract;
        protected TextView vShare;
        protected CardView vCard;

        public AppViewHolder(View v) {
            super(v);

            vName = (TextView) v.findViewById(R.id.txtName);
            vApk = (TextView) v.findViewById(R.id.txtApk);
            vIcon = (ImageView) v.findViewById(R.id.imgIcon);
            vExtract = (TextView) v.findViewById(R.id.btnExtract);
            vShare = (TextView) v.findViewById(R.id.btnShare);
            vCard = (CardView) v.findViewById(R.id.app_card);

        }
    }
}
