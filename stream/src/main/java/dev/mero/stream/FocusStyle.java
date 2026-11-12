package dev.mero.stream;
import android.widget.Button;
import android.graphics.drawable.*;
/** Shared remote focus treatment for native controls. */
final class FocusStyle {
 static void apply(Button b){
  StateListDrawable states=new StateListDrawable();
  for(int state:new int[]{android.R.attr.state_focused,android.R.attr.state_pressed,0}){
   GradientDrawable bg=new GradientDrawable();bg.setColor(state==0?0xff101b27:0xff193d5c);
   bg.setCornerRadius(5*b.getResources().getDisplayMetrics().density);
   bg.setStroke(Math.round((state==0?1:2)*b.getResources().getDisplayMetrics().density),state==0?0xff253647:0xff6abfff);
   states.addState(state==0?new int[]{}:new int[]{state},bg);
  }
  b.setBackground(states);b.setTextColor(0xffd4eeff);b.setAllCaps(false);
 }
}
