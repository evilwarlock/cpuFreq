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

    private boolean              mIsColorSelected = false;
    private Mat                  mRgba;
    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private ColorBlobDetector    mDetector;
    private Mat                  mSpectrum;
    private Size                 SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;
    private Rect                 regionOfInterest;
    private Point                pointOfInterest; // point
    private Scalar               BOUNDING_COLOR;
    private List<Point>          drawnContour;
    private Point                curPoint;
    private Rect                 drawnRect;
    private CPUController        cpuController1;
    private boolean              isTracking;

//    private Path                 pathDrawn;

    private CameraBridgeViewBase mOpenCvCameraView;

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
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255,0,0,255);
        BOUNDING_COLOR = new Scalar(0,0,255,255);
        drawnContour = new ArrayList<Point>(); // save the drawn contour by user
        curPoint = new Point(); // current point on the drawn contour
        drawnRect = new Rect(); // bounding box of the drawn contour by user
        isTracking = false;
//        pathDrawn = new Path();

        cpuController1 = new CPUController();
        cpuController1.CPU_FreqChange(2);
        //System.out.println("here");
        Log.i(TAG, "Power save mode");

    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

//    private float mX, mY;

    public boolean onTouch(View v, MotionEvent event) {

        cpuController1.CPU_FreqChange(4);// Set the frequency to maximum

        // get the real x y value after offset
        int cols = mRgba.cols();
        int rows = mRgba.rows();
        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;
        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;

        Point newTouchPoint = new Point(); // initialize the newTouchPoint
        newTouchPoint.x = (double)x;
        newTouchPoint.y = (double)y;

        // switch event action
        switch (event.getAction()) {

            // when user touch screen
            case MotionEvent.ACTION_DOWN:
                if(!isTracking) { // if it is not tracking, for the first time, it saves touched point
                    curPoint = newTouchPoint; // save the 1st touched point to be current point
                    drawnContour.add(newTouchPoint); // add to List<point> drawnContour
//                Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");
                }



                break;
            // touched moving
            case MotionEvent.ACTION_MOVE:

                drawnContour.add(newTouchPoint); // add newTouchPoint to drawnContour
                Core.line(mRgba, curPoint, newTouchPoint, BOUNDING_COLOR, 3); // draw line for new contour
                curPoint = newTouchPoint; // set newTouchPoint as new current point

                break;
            // when finger up
            case MotionEvent.ACTION_UP:

                MatOfPoint mopDrawnRect = new MatOfPoint(); // initialize MatOfPoint to convert drawnContour from List to MatOfPoint
                mopDrawnRect.fromList(drawnContour); // convert List to MOP
                drawnRect = Imgproc.boundingRect(mopDrawnRect); // get bounding rect for drawnContour

                // check if x y is in image frame
                if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;


                pointOfInterest = new Point(); // new point
                Rect touchedRect = new Rect();

                touchedRect.x = (x>4) ? x-4 : 0;
                touchedRect.y = (y>4) ? y-4 : 0;

                touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
                touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

                // get the regionOfInterest
                regionOfInterest = touchedRect ;

                // get pointOfInterest
                pointOfInterest.x =  (double)x;
                pointOfInterest.y =  (double)y;

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

                mDetector.setHsvColor(mBlobColorHsv);

                Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

                mIsColorSelected = true;

                touchedRegionRgba.release();
                touchedRegionHsv.release();
                break;
        }


        return true; // don't need subsequent touch events
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if (mIsColorSelected) {


            Core.rectangle(mRgba, drawnRect.tl(), drawnRect.br(), CONTOUR_COLOR);

            // new tracking point
            Point trackingPoint = new Point();
//            trackingPoint.x = pointOfInterest.x-50.0;
//            trackingPoint.y = pointOfInterest.y-50.0;

            // draw the touchedRegion
            Core.rectangle(mRgba, regionOfInterest.tl(), regionOfInterest.br(), CONTOUR_COLOR);

            // go to detection
            mDetector.process(mRgba);

            // get contours
            List<MatOfPoint> contours = mDetector.getContours();
            Log.e(TAG, "Contours count: " + contours.size());

            // convert list to array
            List<Rect> finalBox = new ArrayList<Rect>();
            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

            // get boundingBox for each contour
            for (int i = 0; i < contours.size(); i++) {
                finalBox.add(i, Imgproc.boundingRect(contours.get(i)));
//                if(pointOfInterest.inside(finalBox.get(i))||(trackingPoint.inside(finalBox.get(i)))){   // test for trackingPoint & pointOfInterest
                if(pointOfInterest.inside(finalBox.get(i))){

                      Core.rectangle(mRgba, finalBox.get(i).tl(), finalBox.get(i).br(), BOUNDING_COLOR);
                      trackingPoint.x = (finalBox.get(i).tl().x + finalBox.get(i).br().x)/2.0;
                      trackingPoint.y = (finalBox.get(i).tl().y + finalBox.get(i).br().y)/2.0;
//                    Log.e(TAG, "trackingPoint: " + trackingPoint);
//                    Log.e(TAG, "final box: " + finalBox);
                }
                else if(trackingPoint.inside(finalBox.get(i))){

                    Core.rectangle(mRgba, finalBox.get(i).tl(), finalBox.get(i).br(), BOUNDING_COLOR);
                    trackingPoint.x = (finalBox.get(i).tl().x + finalBox.get(i).br().x)/2.0;
                    trackingPoint.y = (finalBox.get(i).tl().y + finalBox.get(i).br().y)/2.0;

                }
            }
                // determine if pointOfInterest is in the boundingBox

            // display the boundingBox
            // draw contours


            // get rectangle including roi&contour
            //Rect finalBox = Imgproc.boundingRect(contours);
            //Core.rectangle(mRgba, finalBox.tl(), finalBox.br(), CONTOUR_COLOR);
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
