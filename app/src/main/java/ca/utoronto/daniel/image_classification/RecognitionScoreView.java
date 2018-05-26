/* Copyright 2015 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package ca.utoronto.daniel.image_classification;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import ca.utoronto.daniel.image_classification.Classifier.Recognition;

import java.util.List;

public class RecognitionScoreView extends View implements ResultsView {
  private static final float TEXT_SIZE_DIP = 24;
  private List<Recognition> results;
  private final float textSizePx;
  private final Paint fgPaint;
  private final Paint bgPaint;

  public RecognitionScoreView(final Context context, final AttributeSet set) {
    super(context, set);

    textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    fgPaint = new Paint();
    fgPaint.setTextSize(textSizePx);

    bgPaint = new Paint();
    bgPaint.setColor(Color.rgb(255, 255, 255));
  }

  @Override
  public void setResults(final List<Recognition> results) {
    this.results = results;
    postInvalidate();
  }

  @Override
  public void onDraw(final Canvas canvas) {
    int x = 140;
    int y = (int) (fgPaint.getTextSize() * 2.0f);

    canvas.drawPaint(bgPaint);

    if (results != null) {
      if (results.isEmpty()){
        x = 140;
        y = (int) (fgPaint.getTextSize() * 2.0f);
        canvas.drawText("No target gesture detected", x, y, fgPaint);
      }
      else{
      for (final Recognition recog : results) {
        float conf = recog.getConfidence();
        String title = recog.getTitle();
        if (conf >= 0.60){
          if (title.equals("do")){bgPaint.setColor(Color.rgb(247, 87, 59));}
          else if (title.equals("re")){bgPaint.setColor(Color.rgb(255, 247, 22));}
          else if (title.equals("mi")){bgPaint.setColor(Color.rgb(80, 255, 22));}
          else if (title.equals("fa")){bgPaint.setColor(Color.rgb(22, 239, 255));}
          else if (title.equals("so")){bgPaint.setColor(Color.rgb(247, 142, 255));}
          canvas.drawPaint(bgPaint);
          canvas.drawText(title +"   detected  !", x, y, fgPaint);
          x -= 50;
          y += (int) (fgPaint.getTextSize() * 2.0f) ;
          canvas.drawText("confidence score: " + String.format("%.4g%n",conf*100.0f) + " %", x, y, fgPaint);}
        else{
          x = 140;
          y = (int) (fgPaint.getTextSize() * 2.0f);
          canvas.drawText("No target gesture detected", x, y, fgPaint);
        }
      }}
    }
    else {
      x = 140;
      y = (int) (fgPaint.getTextSize() * 2.0f);
      canvas.drawText("No target gesture detected", x, y, fgPaint);
    }
  }
}
