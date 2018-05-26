package ca.utoronto.daniel.image_classification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.SystemClock;
import android.util.Size;
import android.util.TypedValue;

import java.util.List;
import java.util.Locale;

public class ClassifierActivity extends CameraActivity implements OnImageAvailableListener {
  private static final Logger LOGGER = new Logger();

  protected static final boolean SAVE_PREVIEW_BITMAP = false;

  private ResultsView resultsView;

  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;

  private long lastProcessingTimeMs;

  // These are the settings for the original v1 Inception model. If you want to
  // use a model that's been produced from the TensorFlow for Poets codelab,
  // you'll need to set IMAGE_SIZE = 299, IMAGE_MEAN = 128, IMAGE_STD = 128,
  // INPUT_NAME = "Mul", and OUTPUT_NAME = "final_result".
  // You'll also need to update the MODEL_FILE and LABEL_FILE paths to point to
  // the ones you produced.
  //
  // To use v3 Inception model, strip the DecodeJpeg Op from your retrained
  // model first:
  //
  // python strip_unused.py \
  // --input_graph=<retrained-pb-file> \
  // --output_graph=<your-stripped-pb-file> \
  // --input_node_names="Mul" \
  // --output_node_names="final_result" \
  // --input_binary=true
  private static final int INPUT_SIZE = 299;
  private static final int IMAGE_MEAN = 128;
  private static final float IMAGE_STD = 128;
  private static final String INPUT_NAME = "Mul";
  private static final String OUTPUT_NAME = "final_result";


  private static final String MODEL_FILE = "file:///android_asset/stripped_graph.pb";
  private static final String LABEL_FILE =
      "file:///android_asset/output_labels.txt";


  private static final boolean MAINTAIN_ASPECT = true;

  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);


  private Integer sensorOrientation;
  private Classifier classifier;
  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;


  private BorderedText borderedText;
  MediaPlayer mp_do = null;
  MediaPlayer mp_re = null;
  MediaPlayer mp_mi = null;
  MediaPlayer mp_fa = null;
  MediaPlayer mp_so = null;

  private String prev_tone = null;

  private void playmusic(final List<Classifier.Recognition> results) {

    if (results != null) {
      if(results.isEmpty()){
        prev_tone = null;
      }
      else{
      for (final Classifier.Recognition recog : results) {
        float conf = recog.getConfidence();
        String title = recog.getTitle();
        LOGGER.i("Tone_Title: %s , title: %s", prev_tone,title);
        if (conf >= 0.60 && !title.equals(prev_tone)){
          prev_tone = title;
          if (title.equals("do")){mp_do.start();}
          else if (title.equals("re")){mp_re.start();}
          else if (title.equals("mi")){mp_mi.start();}
          else if (title.equals("fa")){mp_fa.start();}
          else if (title.equals("so")){mp_so.start();}

          }
        else if (conf < 0.60){
          prev_tone = null;
        }
      }}
    }
    else{
      prev_tone = null;
    }

  }

  @Override
  public synchronized void onDestroy() {
    super.onDestroy();
    if (mp_do != null){
    mp_do.release();}
    if (mp_re != null){
      mp_re.release();}
    if (mp_mi != null){
      mp_mi.release();}
    if (mp_fa != null){
      mp_fa.release();}
    if (mp_so != null){
      mp_so.release();}

  }

  @Override
  public synchronized void onStart() {
    super.onStart();


  }

  @Override
  protected int getLayoutId() {
    return R.layout.camera_connection_fragment;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  private static final float TEXT_SIZE_DIP = 10;

  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    classifier =
        TensorFlowImageClassifier.create(
            getAssets(),
            MODEL_FILE,
            LABEL_FILE,
            INPUT_SIZE,
            IMAGE_MEAN,
            IMAGE_STD,
            INPUT_NAME,
            OUTPUT_NAME);

    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    sensorOrientation = rotation - getScreenOrientation();
    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
    croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Config.ARGB_8888);

    frameToCropTransform = ImageUtils.getTransformationMatrix(
        previewWidth, previewHeight,
        INPUT_SIZE, INPUT_SIZE,
        sensorOrientation, MAINTAIN_ASPECT);

    cropToFrameTransform = new Matrix();
    frameToCropTransform.invert(cropToFrameTransform);
    mp_do = MediaPlayer.create(ClassifierActivity.this, R.raw.mp_do);
    mp_re = MediaPlayer.create(ClassifierActivity.this, R.raw.mp_re);
    mp_mi = MediaPlayer.create(ClassifierActivity.this, R.raw.mp_mi);
    mp_fa = MediaPlayer.create(ClassifierActivity.this, R.raw.mp_fa);
    mp_so = MediaPlayer.create(ClassifierActivity.this, R.raw.mp_so);

    /*addCallback(
        new DrawCallback() {
          @Override
          public void drawCallback(final Canvas canvas) {
            renderDebug(canvas);
          }
        });*/
  }

  @Override
  protected void processImage() {
    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
    final Canvas canvas = new Canvas(croppedBitmap);
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP) {
      ImageUtils.saveBitmap(croppedBitmap);
    }
    runInBackground(
        new Runnable() {
          @Override
          public void run() {
            final long startTime = SystemClock.uptimeMillis();
            final List<Classifier.Recognition> results = classifier.recognizeImage(croppedBitmap);
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
            LOGGER.i("Detect: %s", results);
            cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
            if (resultsView == null) {
              resultsView = (ResultsView) findViewById(R.id.results);
            }
            resultsView.setResults(results);
            playmusic(results);
            //requestRender();
            readyForNextImage();
          }
        });
  }


}
