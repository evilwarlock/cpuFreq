package org.opencv.samples.colorblobdetect;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Point; // import point class

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
//import android.graphics.Path;

public class ColorBlobDetectionActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String  TAG              = "OCVSample::Activity";
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.enableFpsMeter();
                    mOpenCvCameraView.setOnTouchListener(ColorBlobDetectionActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    private boolean              mIsColorSelected = false;
    private boolean              mIsTracking = false;

    private Mat                  mRgba;
    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private ColorBlobDetector    mDetector;
    private ColorBlobDetector    mDetector2;
    private Mat                  mSpectrum;
    private Size                 SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;
    private Scalar               CONTOUR_COLOR2;
    private Point                touchedPoint1; // point touched 1
    private Rect                 touchedRect1; // 4x4 region around touched point 1
    private Point                touchedPoint2; // point touched 2
    private Rect                 touchedRect2; // 4x4 region around touched point 2
    private Scalar               BOUNDING_COLOR;
    private Scalar               BOUNDING_COLOR2;
    private Rect                 drawnRect;
//    private CPUController        cpuController1;
    private int                  counter;

    //    private Path                 pathDrawn;
    private CameraBridgeViewBase mOpenCvCameraView;

    public ColorBlobDetectionActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.color_blob_detection_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
//        mOpenCvCameraView.setMaxFrameSize(320, 240); // set the resolution

    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
//        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgba = new Mat(height, width, CvType.CV_8UC4);

        mDetector = new ColorBlobDetector();
        mDetector2 = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255,0,0,255);
        CONTOUR_COLOR2 = new Scalar(0,0,255,255);
        BOUNDING_COLOR = new Scalar(255,0,0,255);
        BOUNDING_COLOR2 = new Scalar(0,0,255,255);
        drawnRect = new Rect(); // bounding box of the drawn contour by user
        touchedPoint1 = new Point(); // touched point 1
        touchedPoint2 = new Point(); // touched point 2
        touchedRect1 = new Rect(); // touched region 1
        touchedRect2 = new Rect(); // touched region 2

        //initialize cpu controller, set to 400 MHz
//        cpuController1 = new CPUController();
//        cpuController1.CPU_FreqChange(1);
//        System.out.println("here");
//        Log.i(TAG, "Power save mode");

    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {
        //test
//        cpuController1.CPU_FreqChange(1);// Set the frequency to 1134000 KHz

        // get the real x y value after offset
        int cols = mRgba.cols();
        int rows = mRgba.rows();
        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;
        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;


        // check if x y is in image frame
        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        // switch event action
        switch (event.getAction()) {

            // when user touch screen
            case MotionEvent.ACTION_DOWN:
                // get touched point
                Point touchedPoint = new Point();
                touchedPoint.x =  (double)x;
                touchedPoint.y =  (double)y;


                // get pointTouched
                touchedPoint1.x =  (double)x;
                touchedPoint1.y =  (double)y;

                // expand touchRect to 4x4
                Rect touchedRect = new Rect();
                touchedRect.x = (x>4) ? x-4 : 0;
                touchedRect.y = (y>4) ? y-4 : 0;
                touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
                touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

                // get color for touchedRegion
                Mat touchedRegionRgba = mRgba.submat(touchedRect);
                Mat touchedRegionHsv = new Mat();
                Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

                // Calculate average color of touched region
                mBlobColorHsv = Core.sumElems(touchedRegionHsv);
                int pointCount = touchedRect.width*touchedRect.height;
                for (int i = 0; i < mBlobColorHsv.val.length; i++)
                    mBlobColorHsv.val[i] /= pointCount;

                mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

                Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                        ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

                if(!mIsTracking) { // 1st object incoming
                    mDetector.setHsvColor(mBlobColorHsv); // pass HSV value to detector
                    Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

                    touchedPoint1 = touchedPoint ;
                    touchedRect1 = touchedRect ;
                    counter= counter +1;

                }
                else {
                    mDetector2.setHsvColor(mBlobColorHsv); // pass HSV value to detector
                    Imgproc.resize(mDetector2.getSpectrum(), mSpectrum, SPECTRUM_SIZE);
                    touchedRect2 = touchedRect;
                    touchedPoint2 = touchedPoint;
                    counter = counter +1;
                }

                mIsTracking = true;
                mIsColorSelected = true;
                if (counter == 3){
                    mIsColorSelected = false;
//                    cpuController1.CPU_FreqChange(0);// Set the frequency to 1134000 KHz
                }

                touchedRegionRgba.release();
                touchedRegionHsv.release();

                break;
            // touched moving
            case MotionEvent.ACTION_MOVE:

                break;
            // when finger up
            case MotionEvent.ACTION_UP:

                break;
        }


        return false; // need subsequent touch events
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if (mIsColorSelected) {

            Core.rectangle(mRgba, touchedRect1.tl(), touchedRect1.br(), CONTOUR_COLOR); // draw touched rect on object 1

            mDetector.process(mRgba); // process detector

            List<MatOfPoint> contours = mDetector.getContours(); // get contours for obj 1
            Log.e(TAG, "Contours count:  " + contours.size());

            List<Rect> finalBox = new ArrayList<Rect>(); // convert list to array

            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR); // draw contours for object 1

            // draw boundingBox for each contour
            for (int i = 0; i < contours.size(); i++) {
                finalBox.add(i, Imgproc.boundingRect(contours.get(i)));
                if(touchedPoint1.inside(finalBox.get(i))){

                    Core.rectangle(mRgba, finalBox.get(i).tl(), finalBox.get(i).br(), BOUNDING_COLOR);
                    touchedPoint1.x = (finalBox.get(i).tl().x + finalBox.get(i).br().x)/2.0;
                    touchedPoint1.y = (finalBox.get(i).tl().y + finalBox.get(i).br().y)/2.0;
//                    Log.e(TAG, "trackingPoint: " + trackingPoint);
//                    Log.e(TAG, "final box: " + finalBox);
                }

            }

            // track the 2nd object
            if(mIsTracking){
                Core.rectangle(mRgba, touchedRect2.tl(), touchedRect2.br(), CONTOUR_COLOR2); // draw touched rect on object 2

                mDetector2.process(mRgba); // process detector

                List<MatOfPoint> contours2 = mDetector2.getContours(); // get contours for obj 1
                Log.e(TAG, "Contours count2: " + contours2.size());

                List<Rect> finalBox2 = new ArrayList<Rect>(); // convert list to array

                Imgproc.drawContours(mRgba, contours2, -1, CONTOUR_COLOR2); // draw contours for object 1

                // draw boundingBox for each contour
                for (int i = 0; i < contours2.size(); i++) {
                    finalBox2.add(i, Imgproc.boundingRect(contours2.get(i)));
                    if(touchedPoint2.inside(finalBox2.get(i))){

                        Core.rectangle(mRgba, finalBox2.get(i).tl(), finalBox2.get(i).br(), BOUNDING_COLOR2);
                        touchedPoint2.x = (finalBox2.get(i).tl().x + finalBox2.get(i).br().x)/2.0;
                        touchedPoint2.y = (finalBox2.get(i).tl().y + finalBox2.get(i).br().y)/2.0;
//                    Log.e(TAG, "trackingPoint: " + trackingPoint);
//                    Log.e(TAG, "final box: " + finalBox);
                    }

                }
            }

            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);

            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
        }

        return mRgba;
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
}
