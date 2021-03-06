package CameraStuff;

import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.service.avishai.cameratrystuff2.FilesManager;

import java.io.IOException;
import java.util.List;

import static android.content.Context.WINDOW_SERVICE;

/**
 * Created by avishai on 3/6/2017.
 */

public class CameraControl implements CameraControlInterface {

    private MediaRecorder mediaRecorder;
    private Camera mCamera;
    private CameraSurfaceView mView;
    private Context activityContext;
    private long timestamp;

    private static CameraType cameraCurrentState = CameraType.FRONT;

    private static String CAMERA_CONTROL_TAG = "camera control";

    public CameraControl(CameraSurfaceView view, Context context, long creationTime) {
        mView = view;
        this.timestamp = creationTime;

        activityContext = context;
        //init the camera
        //setCameraType(cameraCurrentState);
        //mView.
    }

    public enum CameraType {
        FRONT,
        BACK
    }

    @Override
    public void startRecording() throws Exception {

        startRecording(null);
    }
    public void startRecording(String VideoPath) throws Exception {

        //check preparedness of camera
        if (!prepareCamera(VideoPath)) {

            //can not start recording - return/throw exception
            throw new Exception("Media recorder fails to initialize");
        }

        mediaRecorder.start();
    }

    @Override
    public void stopRecording() {
        mediaRecorder.stop();
        mediaRecorder.reset();

        releaseMediaRecorder();
        //mCamera.lock(); // lock camera for later use
        releaseCamera();
    }

    @Override
    public void swapCamera() {
        if (cameraCurrentState == CameraType.BACK) {
            setCameraType(CameraType.FRONT);
        } else {
            setCameraType(CameraType.BACK);
        }

    }

    @Override
    public void startPreview() {

        //mView.setListener7
        setCameraType(CameraType.BACK);
        cameraCurrentState = CameraType.BACK;
    }

    @Override
    public void releaseCamera() {
        // stop and release camera
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    public boolean isFrontCamera(){
        return  cameraCurrentState == CameraType.FRONT;
    }

    //this function should be called before any other function
    private boolean prepareCamera(String VideoPath) {

        if(mediaRecorder == null)
            mediaRecorder = new MediaRecorder();

        mCamera.unlock();
        mediaRecorder.setCamera(mCamera);

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);


        mediaRecorder.setOutputFormat(2); //mpeg_4//profile.fileFormat);
        mediaRecorder.setVideoFrameRate(60);


        //addded
/*        List<Camera.Size> sizes = mCamera.getParameters().getSupportedVideoSizes();

        Camera.Size SquareSize = chooseVideoAndPictureSize(sizes);

        mediaRecorder.setVideoSize(SquareSize.width, SquareSize.height);*/

        //finish added


        mediaRecorder.setVideoSize(videoSize.width, videoSize.height);
        //mediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        mediaRecorder.setVideoEncoder(profile.videoCodec);
        mediaRecorder.setAudioEncoder(profile.audioCodec);

        //mediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameWidth);
        //mediaRecorder.setProfile(profile);

/*        mediaRecorder.setVideoFrameRate(60);
        mediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mediaRecorder.setVideoEncodingBitRate(profile.audioBitRate);
        mediaRecorder.setVideoEncoder(profile.videoCodec);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);*/

        String fixedFilePath;
        if(VideoPath == null ){
            VideoPath = String.format("/sdcard/slangify%s.mp4", String.valueOf(timestamp));
            fixedFilePath = FilesManager.getFilePath(VideoPath);
        }
        else
            fixedFilePath = VideoPath;

        mediaRecorder.setOutputFile(fixedFilePath);
       // mediaRecorder.setMaxDuration(600000); //set maximum duration 60 sec.
        //mediaRecorder.setMaxFileSize(50000000); //set maximum file size 50M

        if(cameraCurrentState == CameraType.BACK)
            mediaRecorder.setOrientationHint(90);
        else
            mediaRecorder.setOrientationHint(270);


        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            releaseMediaRecorder();
            return false;
        }

        return true;
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset(); // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
        }
    }

    private Camera.Size videoSize;
    private void refreshCamera() {

        //check if there is valid surface to put the camera on
        if (mView.getHolder().getSurface() == null) return;

        // stop preview before making changes
        try {
            // if(mCamera != null)
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        //set the camera to be rotate in the right proportions
        Camera.Parameters parameters = mCamera.getParameters();
        Display display = ((WindowManager) activityContext.getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size previewSize = CameraCalculations.CalculateSquarePreview(previewSizes);//= previewSizes.get(6);
        parameters.setPreviewSize(previewSize.width, previewSize.height);

        //get parameters for video
        List<Camera.Size> videosSizes = parameters.getSupportedVideoSizes();
        videoSize = CameraCalculations.CalculateSquareVideo(videosSizes);

        //apply parameters on camera
        mCamera.setParameters(parameters);

        if (display.getRotation() == Surface.ROTATION_0) {
            //parameters.setPreviewSize(height, width);
            mCamera.setDisplayOrientation(90);

        }
/*        if (display.getRotation() == Surface.ROTATION_270) {
            //parameters.setPreviewSize(width, height);
            mCamera.setDisplayOrientation(180);
        }*/



        //start the preview
        try {
            // create the surface and start camera preview
            if (mCamera != null) {
                mCamera.setPreviewDisplay(mView.getHolder());
                mCamera.startPreview();
            }
        } catch (IOException e) {
            Log.d(CAMERA_CONTROL_TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    private int findCamera(CameraType type) {
        int cameraId = -1;
        // Search for the back facing camera
        // get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        // for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);


            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK &&
                    type == CameraType.BACK) {
                cameraId = i;
                break;
            } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT &&
                    type == CameraType.FRONT) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    private void setCameraType(CameraType type) {

        //release camera before updating the camera type
        releaseCamera();

        int cameraId = findCamera(type);
        if (cameraId >= 0) {

            //Init camera object
            mCamera = Camera.open(cameraId);
            cameraCurrentState = type;

            //refresh after retrieve camera
            refreshCamera();
        }
    }

    /**
     * iterating through array of given sizes.
     * @param choices
     * @return 1080*720 automatically if exists in choices, otherwise will return the next lower 16*9 ratio size.
     * if choices doesn't contain any desired result return the last size in the array.
     */
    private Camera.Size chooseVideoAndPictureSize(List<Camera.Size> choices) {
        if (choices == null) {
            //choices are null, return max values of video / still accordingly to if isVideoSize.
            //return isVideoSize ? new Size(MAX_VIDEO_WIDTH, MAX_VIDEO_HEIGHT) : new Size(MAX_STILL_WIDTH, MAX_STILL_HEIGHT);
        }

        // max width value of the desired aspect ratio (16:9)
        int maxWidth = 720;
        int delta = 100;
        Camera.Size defultSize = choices.get(4);
        for (Camera.Size size : choices) {

            if (size.width <= maxWidth){
                if(size.width == size.height) {
                    return size;
                }
               /* else {
                    int currentDelta = Math.abs(size.width - size.height);
                    if(currentDelta < delta){
                        delta = currentDelta;
                        defultSize= size;
                    }

                }*/

            }

        }

        return choices.get(8);
    }

}
