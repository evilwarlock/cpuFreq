package org.opencv.samples.colorblobdetect;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

//import android.annotation.SuppressLint;
//import android.bluetooth.BluetoothAdapter;
import android.annotation.SuppressLint;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.*;
import org.opencv.android.*;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.*;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Scalar;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View.OnTouchListener;
//import org.opencv.objdetect.CascadeClassifier;
import org.opencv.video.Video;
//import android.graphics.Path;

public class ColorBlobDetectionActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String  TAG              = "OCVSample::Activity";

    public static final int      VIEW_MODE_RGBA      = 0;
//    public static final int      VIEW_MODE_HIST      = 1;
//    public static final int      VIEW_MODE_CANNY     = 2;
//    public static final int      VIEW_MODE_SEPIA     = 3;
    public static final int 	 VIEW_MODE_OPTICAL   = 4;
//    public static final int      VIEW_MODE_HOG       = 5;
//    public static final int 	 VIEW_MODE_NETWORK   = 6;
//    public static final int 	 VIEW_MODE_SEND      = 7;

//    private static final int 	 MAX_CORNERS = 50;
//    private static final int 	 REQUEST_ENABLE_BT = 10;
    // Name for the SDP record when creating server socket
//    private static final String NAME = "CameraNetwork";
    // Unique UUID for this application
//    private BluetoothAdapter mBluetoothAdapter = null;
//    static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
//    static String address = "30:19:66:CF:EA:9A";


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

//    private Mat                  mRgba;
//    private Mat prevRGB, nextRGB;
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
//    private Rect                 drawnRect;

    private MenuItem 			 mItemPreviewRGBA;
    private MenuItem 			 mItemPreviewOptical;

    public static int           viewMode = VIEW_MODE_RGBA;

//    private CascadeClassifier mCascade;

    private boolean bShootNow = false, bDisplayTitle = true,  bFirstFaceSaved = false;

    private byte[] byteColourTrackCentreHue;

//    private double d, dTextScaleFactor, x1, x2, y1, y2;

//    private double[] vecHoughLines;

    private Point pt, pt1, pt2;

    private int  x, y, radius, iMinRadius, iMaxRadius, iCannyLowerThreshold,
            iCannyUpperThreshold, iAccumulator, iLineThickness = 3,
            iHoughLinesThreshold = 50, iHoughLinesMinLineSize = 20,
            iHoughLinesGap = 20, iMaxFaceHeight, iMaxFaceHeightIndex,
            iFileOrdinal = 0, iCamera = 0, iNumberOfCameras = 0, iGFFTMax = 40,
            iContourAreaMin = 1000;

//    private JavaCameraView mOpenCvCameraView0;
//    private JavaCameraView mOpenCvCameraView1;

    private List<Byte> byteStatus;
    private List<Integer> iHueMap, channels;
    private List<Float> ranges;
    private List<Point> pts, corners, cornersThis, cornersPrev;
    private List<MatOfPoint> contours;

    private long lFrameCount = 0, lMilliStart = 0, lMilliNow = 0, lMilliShotTime = 0;

    private Mat mRgba, mGray, mIntermediateMat, mMatRed, mMatGreen, mMatBlue, mROIMat,
            mMatRedInv, mMatGreenInv, mMatBlueInv, mHSVMat, mErodeKernel, mContours,
            lines, mFaceDest, mFaceResized, matOpFlowPrev, matOpFlowThis,
            matFaceHistogramPrevious, matFaceHistogramThis, mHist;

    private MatOfFloat mMOFerr, MOFrange;
    private MatOfRect faces;
    private MatOfByte mMOBStatus;
    private MatOfPoint2f mMOP2f1, mMOP2f2, mMOP2fptsPrev, mMOP2fptsThis, mMOP2fptsSafe;
    private MatOfPoint2f mApproxContour;
    private MatOfPoint MOPcorners;
    private MatOfInt MOIone, histSize;

    private Rect rect, rDest;

    private Scalar colorRed, colorGreen;
    private Size sSize, sSize3, sSize5, sMatSize;
    private String string, sShotText;





    //    private CPUController        cpuController1;
    private int                  counter;
    private int                  count = 0;


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
        mOpenCvCameraView.setMaxFrameSize(640, 480); // set the resolution

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
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");

        mItemPreviewRGBA  = menu.add("Preview RGBA");
        mItemPreviewOptical = menu.add("Optical Flow Tracker");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item == mItemPreviewRGBA)
            viewMode = VIEW_MODE_RGBA;
        else if (item == mItemPreviewOptical)
            viewMode = VIEW_MODE_OPTICAL;

        return true;
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
//        drawnRect = new Rect(); // bounding box of the drawn contour by user
        touchedPoint1 = new Point(); // touched point 1
        touchedPoint2 = new Point(); // touched point 2
        touchedRect1 = new Rect(); // touched region 1
        touchedRect2 = new Rect(); // touched region 2
//        nextRGB = new Mat();
        // TODO Auto-generated method stub
        byteColourTrackCentreHue = new byte[3];
        // green = 60 // mid yellow  27
        byteColourTrackCentreHue[0] = 27;
        byteColourTrackCentreHue[1] = 100;
        byteColourTrackCentreHue[2] = (byte)255;
        byteStatus = new ArrayList<Byte>();

        channels = new ArrayList<Integer>();
        channels.add(0);
        colorRed = new Scalar(255, 0, 0, 255);
        colorGreen = new Scalar(0, 255, 0, 255);
        contours = new ArrayList<MatOfPoint>();
        corners = new ArrayList<Point>();
        cornersThis = new ArrayList<Point>();
        cornersPrev = new ArrayList<Point>();

        faces = new MatOfRect();

        histSize = new MatOfInt(25);

        iHueMap = new ArrayList<Integer>();
        iHueMap.add(0);
        iHueMap.add(0);
        lines = new Mat();

        mApproxContour = new MatOfPoint2f();
        mContours = new Mat();
        mHist = new Mat();
        mGray = new Mat();
        mHSVMat = new Mat();
        mIntermediateMat = new Mat();
        mMatRed = new Mat();
        mMatGreen = new Mat();
        mMatBlue = new Mat();
        mMatRedInv = new Mat();
        mMatGreenInv = new Mat();
        mMatBlueInv = new Mat();
        MOIone = new MatOfInt(0);

        MOFrange = new MatOfFloat(0f, 256f);
        mMOP2f1 = new MatOfPoint2f();
        mMOP2f2 = new MatOfPoint2f();
        mMOP2fptsPrev = new MatOfPoint2f();
        mMOP2fptsThis = new MatOfPoint2f();
        mMOP2fptsSafe = new MatOfPoint2f();
        mMOFerr = new MatOfFloat();
        mMOBStatus = new MatOfByte();
        MOPcorners = new MatOfPoint();

        mROIMat = new Mat();
        mFaceDest = new Mat();
        mFaceResized = new Mat();
        matFaceHistogramPrevious = new Mat();
        matFaceHistogramThis= new Mat();
        matOpFlowThis = new Mat();
        matOpFlowPrev= new Mat();

        pt = new Point (0, 0);
        pt1 = new Point (0, 0);
        pt2 = new Point (0, 0);

        pts = new ArrayList<Point>();

        ranges = new ArrayList<Float>();
        ranges.add(50.0f);
        ranges.add(256.0f);
        rect = new Rect();
        rDest = new Rect();

        sMatSize = new Size();
        sSize = new Size();
        sSize3 = new Size(3, 3);
        sSize5 = new Size(5, 5);

        string = "";

        DisplayMetrics dm = this.getResources().getDisplayMetrics();
        int densityDpi = dm.densityDpi;
//        dTextScaleFactor = ((double)densityDpi / 240.0) * 0.9;

        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);


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


        Size sizeRgba = mRgba.size();

        Mat rgbaInnerWindow, rgbaInnerWindow1, rgbaInnerWindow2;
        Mat cellWindow;
        MatOfByte status = new MatOfByte();
        MatOfFloat err = new MatOfFloat();
        MatOfPoint2f nextPts =new MatOfPoint2f();
        MatOfPoint2f prevPts =new MatOfPoint2f();
        MatOfPoint initial = new MatOfPoint();

        int rows = (int) sizeRgba.height;
        int cols = (int) sizeRgba.width;

        int left = 2*cols / 8;
        int top = 2*rows / 8;

        int width = cols * 1 / 4;
        int height = rows * 1 / 4;

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

//        return mRgba;


        switch (ColorBlobDetectionActivity.viewMode) {
            case ColorBlobDetectionActivity.VIEW_MODE_RGBA:
                break;

            case ColorBlobDetectionActivity.VIEW_MODE_OPTICAL:

                if (mMOP2fptsPrev.rows() == 0) {

                    //Log.d("Baz", "First time opflow");
                    // first time through the loop so we need prev and this mats
                    // plus prev points
                    // get this mat
                    Imgproc.cvtColor(mRgba, matOpFlowThis, Imgproc.COLOR_RGBA2GRAY);

                    // copy that to prev mat
                    matOpFlowThis.copyTo(matOpFlowPrev);

                    // get prev corners
                    Imgproc.goodFeaturesToTrack(matOpFlowPrev, MOPcorners, iGFFTMax, 0.05, 20);
                    mMOP2fptsPrev.fromArray(MOPcorners.toArray());

                    // get safe copy of this corners
                    mMOP2fptsPrev.copyTo(mMOP2fptsSafe);
                }
                else
                {
                    //Log.d("Baz", "Opflow");
                    // we've been through before so
                    // this mat is valid. Copy it to prev mat
                    matOpFlowThis.copyTo(matOpFlowPrev);

                    // get this mat
                    Imgproc.cvtColor(mRgba, matOpFlowThis, Imgproc.COLOR_RGBA2GRAY);

                    // get the corners for this mat
                    Imgproc.goodFeaturesToTrack(matOpFlowThis, MOPcorners, iGFFTMax, 0.05, 20);
                    mMOP2fptsThis.fromArray(MOPcorners.toArray());

                    // retrieve the corners from the prev mat
                    // (saves calculating them again)
                    mMOP2fptsSafe.copyTo(mMOP2fptsPrev);

                    // and save this corners for next time through

                    mMOP2fptsThis.copyTo(mMOP2fptsSafe);
                }


           	/*
           	Parameters:
           		prevImg first 8-bit input image
           		nextImg second input image
           		prevPts vector of 2D points for which the flow needs to be found; point coordinates must be single-precision floating-point numbers.
           		nextPts output vector of 2D points (with single-precision floating-point coordinates) containing the calculated new positions of input features in the second image; when OPTFLOW_USE_INITIAL_FLOW flag is passed, the vector must have the same size as in the input.
           		status output status vector (of unsigned chars); each element of the vector is set to 1 if the flow for the corresponding features has been found, otherwise, it is set to 0.
           		err output vector of errors; each element of the vector is set to an error for the corresponding feature, type of the error measure can be set in flags parameter; if the flow wasn't found then the error is not defined (use the status parameter to find such cases).
            */
                Video.calcOpticalFlowPyrLK(matOpFlowPrev, matOpFlowThis, mMOP2fptsPrev, mMOP2fptsThis, mMOBStatus, mMOFerr);

                cornersPrev = mMOP2fptsPrev.toList();
                cornersThis = mMOP2fptsThis.toList();
                byteStatus = mMOBStatus.toList();

                y = byteStatus.size() - 1;

                for (x = 0; x < y; x++) {
                    if (byteStatus.get(x) == 1) {
                        pt = cornersThis.get(x);
                        pt2 = cornersPrev.get(x);

                        Core.circle(mRgba, pt, 5, colorRed, iLineThickness - 1);

                        Core.line(mRgba, pt, pt2, colorRed, iLineThickness);
                    }
                }

                //Log.d("Baz", "Opflow feature count: "+x);
                if (bDisplayTitle)
//                    ShowTitle ("Optical Flow", 1, colorGreen);

                break;
        }

        // get the time now in every frame
        lMilliNow = System.currentTimeMillis();

        // update the frame counter
        lFrameCount++;

        if (bDisplayTitle) {
            string = String.format("FPS: %2.1f", (float)(lFrameCount * 1000) / (float)(lMilliNow - lMilliStart));

//            ShowTitle (string, 2, colorGreen);
        }

        if (bShootNow) {
            // get the time of the attempt to save a screenshot
            lMilliShotTime = System.currentTimeMillis();
            bShootNow = false;

            // try it, and set the screen text accordingly.
            // this text is shown at the end of each frame until
            // 1.5 seconds has elapsed
            if (SaveImage (mRgba)) {
                sShotText = "SCREENSHOT SAVED";
            }
            else {
                sShotText = "SCREENSHOT FAILED";
            }

        }

        if (System.currentTimeMillis() - lMilliShotTime < 1500)
            sShotText = "SCREENSHOT FAILED";
//            ShowTitle (sShotText, 3, colorRed);

        return mRgba;
    }

    public boolean onTouchEvent(final MotionEvent event) {

        bShootNow = true;
        return false; // don't need more than one touch event

    }


    public void DrawCross (Mat mat, Scalar color, Point pt) {
        int iCentreCrossWidth = 24;

        pt1.x = pt.x - (iCentreCrossWidth >> 1);
        pt1.y = pt.y;
        pt2.x = pt.x + (iCentreCrossWidth >> 1);
        pt2.y = pt.y;

        Core.line(mat, pt1, pt2, color, iLineThickness - 1);

        pt1.x = pt.x;
        pt1.y = pt.y + (iCentreCrossWidth >> 1);
        pt2.x = pt.x;
        pt2.y = pt.y  - (iCentreCrossWidth >> 1);

        Core.line(mat, pt1, pt2, color, iLineThickness - 1);

    }


    public Mat getHistogram (Mat mat) {
        Imgproc.calcHist(Arrays.asList(mat), MOIone, new Mat(), mHist, histSize, MOFrange);

        Core.normalize(mHist, mHist);

        return mHist;
    }

    @SuppressLint("SimpleDateFormat")
    public boolean SaveImage (Mat mat) {

        Imgproc.cvtColor(mat, mIntermediateMat, Imgproc.COLOR_RGBA2BGR, 3);

        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        String filename = "OpenCV_";
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Date date = new Date(System.currentTimeMillis());
        String dateString = fmt.format(date);
        filename += dateString + "-" + iFileOrdinal;
        filename += ".png";

        File file = new File(path, filename);

        Boolean bool = null;
        filename = file.toString();
        bool = Highgui.imwrite(filename, mIntermediateMat);

        //if (bool == false)
        //Log.d("Baz", "Fail writing image to external storage");

        return bool;
.
    }



//    private void ShowTitle (String s, int iLineNum, Scalar color) {
//        Core.putText(mRgba, s, new Point(10, (int)(dTextScaleFactor * 60 * iLineNum)),
//                Core.FONT_HERSHEY_SIMPLEX, dTextScaleFactor, color, 2);
//    }





    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
}
