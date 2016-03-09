package com.ReactCamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.RelativeLayout;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.uimanager.ThemedReactContext;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import me.dm7.barcodescanner.core.CameraPreview;
import me.dm7.barcodescanner.core.CameraUtils;
import me.dm7.barcodescanner.core.IViewFinder;
import me.dm7.barcodescanner.core.ViewFinderView;

/**
 * Created by northfoxz on 2016/1/7.
 */
public abstract class RNCameraInstanceView extends RelativeLayout implements Camera.PreviewCallback {
    private Camera mCamera;
    private RelativeLayout mCameraView;
    private CameraPreview mPreview;
    private IViewFinder mViewFinderView;
    private Rect mFramingRectInPreview;
    private ThemedReactContext mContext;
    private boolean mViewFinderDisplay;

    public RNCameraInstanceView(ThemedReactContext context) {
        super(context);
        mContext = context;
        this.setupLayout();
    }

    public final void setupLayout() {
        this.mPreview = new CameraPreview(this.getContext());
        RelativeLayout mCameraView = new RelativeLayout(this.getContext());
        mCameraView.setGravity(17);
        mCameraView.setBackgroundColor(-16777216);
        mCameraView.addView(this.mPreview);
        this.addView(mCameraView);
        this.mViewFinderView = this.createViewFinderView(this.getContext());
        if(this.mViewFinderView instanceof View) {

        } else {
            throw new IllegalArgumentException("IViewFinder object returned by \'createViewFinderView()\' should be instance of android.view.View");
        }
    }

    public void setViewFinderDisplay(boolean display) {
        if(display) {
            Log.w("camera", "setting view finder display as true");
            this.addView((View)this.mViewFinderView);
        }else
            Log.w("camera", "setting view finder display as false");
        mViewFinderDisplay = display;
    }

    protected IViewFinder createViewFinderView(Context context) {
        return new ViewFinderView(context);
    }

    public void startCamera(int cameraId) {
        this.startCamera(CameraUtils.getCameraInstance(cameraId));
    }
    public void startCamera() {
        this.startCamera(CameraUtils.getCameraInstance());
    }
    public void startCamera(Camera camera) {
        this.mCamera = camera;
        if(this.mCamera != null) {
            this.mViewFinderView.setupViewFinder();
            this.mPreview.setCamera(this.mCamera, this);

            //STEP #1: Get rotation degrees
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
            int rotation = 0;
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0: degrees = 0; break; //Natural orientation
                case Surface.ROTATION_90: degrees = 90; break; //Landscape left
                case Surface.ROTATION_180: degrees = 180; break;//Upside down
                case Surface.ROTATION_270: degrees = 270; break;//Landscape right
            }
            int rotate = (info.orientation - degrees + 360) % 360;

//STEP #2: Set the 'rotation' parameter
            Camera.Parameters params = mCamera.getParameters();
            params.setRotation(rotate);
            mCamera.setParameters(params);
            this.mPreview.initCameraPreview();
        }

    }

    public void stopCamera() {
        if(this.mCamera != null) {
            this.mPreview.stopCameraPreview();
            this.mPreview.setCamera((Camera)null, (Camera.PreviewCallback)null);
            this.mCamera.release();
            this.mCamera = null;
        }

    }

    public synchronized Rect getFramingRectInPreview(int previewWidth, int previewHeight) {
        if(this.mFramingRectInPreview == null) {
            Rect framingRect = this.mViewFinderView.getFramingRect();
            int viewFinderViewWidth = this.mViewFinderView.getWidth();
            int viewFinderViewHeight = this.mViewFinderView.getHeight();
            if(framingRect == null || viewFinderViewWidth == 0 || viewFinderViewHeight == 0) {
                return null;
            }

            Rect rect = new Rect(framingRect);
            rect.left = rect.left * previewWidth / viewFinderViewWidth;
            rect.right = rect.right * previewWidth / viewFinderViewWidth;
            rect.top = rect.top * previewHeight / viewFinderViewHeight;
            rect.bottom = rect.bottom * previewHeight / viewFinderViewHeight;
            this.mFramingRectInPreview = rect;
        }

        return this.mFramingRectInPreview;
    }

    public void setFlash(boolean flag) {
        if(this.mCamera != null && CameraUtils.isFlashSupported(this.mCamera)) {
            Camera.Parameters parameters = this.mCamera.getParameters();
            if(flag) {
                if(parameters.getFlashMode().equals("torch")) {
                    return;
                }

                parameters.setFlashMode("torch");
            } else {
                if(parameters.getFlashMode().equals("off")) {
                    return;
                }

                parameters.setFlashMode("off");
            }

            this.mCamera.setParameters(parameters);
        }

    }

    public boolean getFlash() {
        if(this.mCamera != null && CameraUtils.isFlashSupported(this.mCamera)) {
            Camera.Parameters parameters = this.mCamera.getParameters();
            return parameters.getFlashMode().equals("torch");
        } else {
            return false;
        }
    }

    public void toggleFlash() {
        if(this.mCamera != null && CameraUtils.isFlashSupported(this.mCamera)) {
            Camera.Parameters parameters = this.mCamera.getParameters();
            if(parameters.getFlashMode().equals("torch")) {
                parameters.setFlashMode("off");
            } else {
                parameters.setFlashMode("torch");
            }

            this.mCamera.setParameters(parameters);
        }

    }

    public void setAutoFocus(boolean state) {
        if(this.mPreview != null) {
            this.mPreview.setAutoFocus(state);
        }

    }

    public void takePicture(final ReadableMap options) {
        // get an image from the camera
        mCamera.takePicture(null, null, new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                File pictureFile = getOutputMediaFile(options.getString("androidPath"));
                if (pictureFile == null){
                    returnPictureTakenResult("error", "directory error");
                    mCamera.startPreview();
                    return;
                }

                Bitmap image = BitmapFactory.decodeByteArray(data, 0, data.length);

                try {

                    // Extract metadata.
                    final Metadata metadata = ImageMetadataReader.readMetadata(new BufferedInputStream(new ByteArrayInputStream(data)), false);    // true for streaming

                    // Get the EXIF orientation.
                    final ExifIFD0Directory exifIFD0Directory = metadata.getDirectory(ExifIFD0Directory.class);
                    if(exifIFD0Directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION))
                    {
                        final int exifOrientation = exifIFD0Directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);

                        final Matrix bitmapMatrix = new Matrix();
                        switch(exifOrientation)
                        {
                            case 1:                                                                                     break;  // top left
                            case 2:                                                 bitmapMatrix.postScale(-1, 1);      break;  // top right
                            case 3:         bitmapMatrix.postRotate(180);                                               break;  // bottom right
                            case 4:         bitmapMatrix.postRotate(180);           bitmapMatrix.postScale(-1, 1);      break;  // bottom left
                            case 5:         bitmapMatrix.postRotate(90);            bitmapMatrix.postScale(-1, 1);      break;  // left top
                            case 6:         bitmapMatrix.postRotate(90);                                                break;  // right top
                            case 7:         bitmapMatrix.postRotate(270);           bitmapMatrix.postScale(-1, 1);      break;  // right bottom
                            case 8:         bitmapMatrix.postRotate(270);                                               break;  // left bottom
                            default:                                                                                    break;  // Unknown
                        }

                        // Create new bitmap.
                        image = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), bitmapMatrix, false);
                    }

                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    image.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();

                    if (options.hasKey("metadata") && options.getMap("metadata").hasKey("location")) {
                        double timestamp = options.getMap("metadata").getMap("location").getDouble("timestamp");
                        ReadableMap location = options.getMap("metadata").getMap("location").getMap("coords");
                        ExifInterface exif = new ExifInterface(pictureFile.getAbsolutePath());



                        String altitudeRef;
                        double altitude = location.getDouble("altitude");
                        if (location.getDouble("altitude") < 0) {
                            altitudeRef = "1";
                            altitude = -altitude;
                        } else {
                            altitudeRef = "0";
                        }
                        exif.setAttribute("GPSAltitude", String.valueOf(altitude));
                        exif.setAttribute("GPSAltitudeRef", altitudeRef);

                        String latitudeRef;
                        double latitude = location.getDouble("altitude");
                        if (location.getDouble("latitude") < 0) {
                            latitudeRef = "S";
                            latitude = -latitude;
                        } else {
                            latitudeRef = "N";
                        }
                        exif.setAttribute("GPSLatitude", String.valueOf(latitude));
                        exif.setAttribute("GPSLatitudeRef", latitudeRef);


                        String longitudeRef;
                        double longitude = location.getDouble("altitude");
                        if (location.getDouble("longitude") < 0) {
                            longitudeRef = "W";
                            longitude = -longitude;
                        } else {
                            longitudeRef = "E";
                        }
                        exif.setAttribute("GPSLongitude", String.valueOf(longitude));
                        exif.setAttribute("GPSLongitudeRef", longitudeRef);

                        Calendar initialDate = Calendar.getInstance();
                        initialDate.setTimeInMillis((long) timestamp);
                        initialDate.setTimeZone(TimeZone.getTimeZone("UTC"));

                        SimpleDateFormat ts = new SimpleDateFormat("HH:mm:ss.SSSSSS");
                        SimpleDateFormat ds = new SimpleDateFormat("yyyy:MM:dd");

                        exif.setAttribute("GPSTimeStamp", ts.format(initialDate.getTime()));
                        exif.setAttribute("GPSDateStamp", ds.format(initialDate.getTime()));

                        exif.saveAttributes();
                    }

                    // Restart the camera preview.
                    returnPictureTakenResult("success", pictureFile.getAbsolutePath());
                    mCamera.startPreview();
                } catch (FileNotFoundException e) {
                    returnPictureTakenResult("error", "file not found");
                    e.printStackTrace();
                } catch (IOException e) {
                    returnPictureTakenResult("error", "an exception error");
                    e.printStackTrace();
                } catch (ImageProcessingException e) {
                    returnPictureTakenResult("error", "an ip exception error");
                    e.printStackTrace();
                } catch (MetadataException e) {
                    returnPictureTakenResult("error", "an md exception error");
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Used to return the camera File output.
     * @return
     */
    private File getOutputMediaFile(String path){
        try {

            // Create a media file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String root = Environment.getExternalStorageDirectory().toString();
            File myDir = new File(path);
            myDir.mkdirs();
            if(myDir.exists())
                Log.v("camera", "directory created");
            else
                Log.v("camera", "directory still not created");
            File mediaFile;
            mediaFile = new File(myDir, "IMG_"+ timeStamp + ".jpg");
            mediaFile.createNewFile( );
            if(mediaFile.exists())
                Log.v("camera", "file created now");
            else
                Log.v("camera", "file still not created");

            if(mediaFile.isDirectory())
                Log.v("camera", "is directory");
            else
                Log.v("camera", "is file");
            Log.v("camera", mediaFile.getAbsolutePath());
            return mediaFile;
        }
        catch(SecurityException e) {
            Log.v("camera", e.getMessage());
            return null;
        }
        catch(IOException e) {
            Log.v("camera", e.getMessage());
            return null;
        }
    }

    public void returnPictureTakenResult(String resultType, String resultMessage) {

    }
}
