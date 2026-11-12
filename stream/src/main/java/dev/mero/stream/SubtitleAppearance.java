package dev.mero.stream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.*;
import androidx.media3.common.text.Cue;
import androidx.media3.ui.CaptionStyleCompat;
import androidx.media3.ui.SubtitleView;
import java.util.Collections;

/** Local caption preferences, separate from episode progress and source tracks. */
@androidx.media3.common.util.UnstableApi
final class SubtitleAppearance {
    static final String[] SIZE={"Small","Medium","Large","Extra large"};
    static final String[] COLOR={"White","Yellow","Cyan"};
    static final String[] BACKGROUND={"None","Translucent black","Black"};
    static final String[] EDGE={"Outline","Shadow","None"};
    final SharedPreferences prefs;
    final Activity activity;
    SubtitleAppearance(Activity activity){this.activity=activity;prefs=activity.getSharedPreferences("subtitle-appearance",0);}
    int choice(String key,int count){return Math.max(0,Math.min(count-1,prefs.getInt(key,key.equals("size")?1:0)));}
    void apply(SubtitleView subtitles){
        if(subtitles==null)return;
        int foreground=new int[]{Color.WHITE,0xffffe36a,0xff8ceaff}[choice("color",3)];
        int background=new int[]{Color.TRANSPARENT,0x99000000,Color.BLACK}[choice("background",3)];
        int edge=new int[]{CaptionStyleCompat.EDGE_TYPE_OUTLINE,CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,CaptionStyleCompat.EDGE_TYPE_NONE}[choice("edge",3)];
        subtitles.setApplyEmbeddedStyles(false);
        subtitles.setApplyEmbeddedFontSizes(false);
        subtitles.setStyle(new CaptionStyleCompat(foreground,background,Color.TRANSPARENT,edge,Color.BLACK,Typeface.DEFAULT_BOLD));
        subtitles.setFractionalTextSize(new float[]{.035f,.045f,.055f,.065f}[choice("size",4)]);
    }
    void preview(SubtitleView preview){apply(preview);preview.setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,new float[]{18,22,26,30}[choice("size",4)]);}
    AlertDialog show(SubtitleView target){
        LinearLayout content=new LinearLayout(activity);content.setOrientation(LinearLayout.VERTICAL);
        SubtitleView preview=new SubtitleView(activity);preview.setBackgroundColor(0xff24384a);
        preview.setCues(Collections.singletonList(new Cue.Builder().setText("Subtitle preview — your style").build()));
        preview(preview);
        content.addView(preview,new LinearLayout.LayoutParams(-1,Math.round(110*activity.getResources().getDisplayMetrics().density)));
        String[] keys={"size","color","background","edge"};String[] names={"Font size","Text color","Background","Edge"};
        String[][] values={SIZE,COLOR,BACKGROUND,EDGE};
        for(int i=0;i<keys.length;i++){
            String key=keys[i],name=names[i];String[] labels=values[i];
            Button b=new Button(activity);FocusStyle.apply(b);b.setText(name+": "+labels[choice(key,labels.length)]);
            b.setOnClickListener(v->{prefs.edit().putInt(key,(choice(key,labels.length)+1)%labels.length).apply();apply(target);preview(preview);b.setText(name+": "+labels[choice(key,labels.length)]);});
            content.addView(b);
        }
        ScrollView scroll=new ScrollView(activity);scroll.addView(content);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Subtitle appearance").setView(scroll).setNegativeButton("Done",null).create();
        dialog.show();content.getChildAt(1).requestFocus();return dialog;
    }
}
