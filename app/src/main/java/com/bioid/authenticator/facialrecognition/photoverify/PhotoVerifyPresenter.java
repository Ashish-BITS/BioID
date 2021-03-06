package com.bioid.authenticator.facialrecognition.photoverify;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.bioid.authenticator.base.logging.LoggingHelperFactory;
import com.bioid.authenticator.base.network.bioid.webservice.MovementDirection;
import com.bioid.authenticator.base.network.bioid.webservice.token.NoopToken;
import com.bioid.authenticator.facialrecognition.FacialRecognitionBasePresenter;
import com.bioid.authenticator.facialrecognition.FacialRecognitionFragment;

/**
 * Presenter for the {@link FacialRecognitionFragment} doing photo verify.
 */
public class PhotoVerifyPresenter extends FacialRecognitionBasePresenter<NoopToken> {
    /**
     * This delay makes it more likely to have a stable auto focus and white balance setup.
     * Because the liveness detection process does start as soon as a face was found, which can be very quickly, this delay was introduced.
     * Not having this delay could lead to false positives in the motion detection algorithm.
     */
    private static final int AUTO_FOCUS_AND_WHITE_BALANCE_DELAY_IN_MILLIS = 500;
    private static final int DELAY_TO_CONTINUE_WITHIN_CHALLENGE_IN_MILLIS = 2_000;

    private Bitmap[] selfies = new Bitmap[2];

    public PhotoVerifyPresenter(Context ctx, FacialRecognitionFragment view) {
        super(ctx, LoggingHelperFactory.create(PhotoVerifyPresenter.class), view);
    }

    @Override
    protected void startBiometricOperation() {
        log.i("startBiometricOperation()");

        view.showInitialisationInfo();
        view.resetMovementIndicator();

        backgroundHandler.runWithDelay(this::detectFace, AUTO_FOCUS_AND_WHITE_BALANCE_DELAY_IN_MILLIS);
    }

    @Override
    protected void onFaceDetected() {
        log.d("onFaceDetected()");

        captureImagePair(0, MovementDirection.any, MovementDirection.any);
    }

    @Override
    protected void onNoFaceDetected() {
        log.d("onNoFaceDetected()");

        // start the verification process anyway, relying on the BioID server face detection
        captureImagePair(0, MovementDirection.any, MovementDirection.any);
    }

    @Override
    protected void onReferenceImageCaptured(@NonNull final Bitmap bitmap) {
        log.d("onReferenceImageCaptured(img=%s)", bitmap);

        // create motion detection template within the background to keep the UI responsive
        backgroundHandler.runOnBackgroundThread(
                () -> motionDetection.createTemplate(bitmap),
                () -> {
                    selfies[0] = bitmap;

                    backgroundHandler.runWithDelay(() -> {
                        // waiting for images with motion using timeout
                        imageDetectionState = ImageDetectionState.WAITING_FOR_IMAGE_WITH_MOTION;
                        setupMotionTimeout();
                    }, DELAY_TO_CHECK_FOR_MOTION_IN_MILLIS);
                }, e -> {
                    throw e;  // should lead to app crash
                }, null);
    }

    @Override
    protected void onImageWithMotionProcessed() {}

    @Override
    protected void onImageWithMotionCaptured(@NonNull final Bitmap bitmap) {
        log.d("onImageWithMotionCaptured(img=%s)", bitmap);

        // cancel motion timeout and hide movement instruction text
        backgroundHandler.cancelScheduledTask(taskIdMotionTimeout);
        view.hideMessages();
        selfies[1] = bitmap;

        view.hideMovementIndicator();
        ((PhotoVerifyActivity) view.getParentActivity()).endSelfiesSession(selfies);
    }

    @Override
    protected void onUploadSuccessful() {}

    @Override
    protected void onUploadFailed(RuntimeException e) {}

    @Override
    public void promptForProcessExplanationAccepted() {
        throw new IllegalStateException("promptForProcessExplanationAccepted() called on PhotoVerifyPresenter");
    }

    @Override
    public void promptForProcessExplanationRejected() {
        throw new IllegalStateException("promptForProcessExplanationRejected() called on PhotoVerifyPresenter");
    }

    @Override
    public void promptToTurn90DegreesAccepted() {
        throw new IllegalStateException("promptToTurn90DegreesAccepted() called on PhotoVerifyPresenter");
    }

    @Override
    public void promptToTurn90DegreesRejected() {
        throw new IllegalStateException("promptToTurn90DegreesRejected() called on PhotoVerifyPresenter");
    }

    @Override
    protected void resetBiometricOperation() {
        super.resetBiometricOperation();
    }
}