package com.bioid.authenticator.facialrecognition.enrollment;

import android.annotation.SuppressLint;
import android.content.Context;

import com.bioid.authenticator.base.image.Yuv420Image;
import com.bioid.authenticator.base.logging.LoggingHelper;
import com.bioid.authenticator.base.network.bioid.webservice.BioIdWebserviceClient;
import com.bioid.authenticator.base.network.bioid.webservice.MovementDirection;
import com.bioid.authenticator.base.network.bioid.webservice.token.EnrollmentToken;
import com.bioid.authenticator.base.network.bioid.webservice.token.EnrollmentTokenProvider;
import com.bioid.authenticator.base.threading.BackgroundHandler;
import com.bioid.authenticator.facialrecognition.FacialRecognitionContract;
import com.bioid.authenticator.testutil.SynchronousBackgroundHandler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EnrollmentPresenterTest {

    private static final EnrollmentToken ENROLLMENT_TOKEN = new EnrollmentToken("enrollment");

    @Mock
    private Context ctx;
    @Mock
    private LoggingHelper log;
    @Mock
    private FacialRecognitionContract.View view;
    @Spy
    private SynchronousBackgroundHandler backgroundHandler;
    @Mock
    private EnrollmentTokenProvider tokenProvider;
    @Mock
    private BioIdWebserviceClient bioIdWebserviceClient;

    @Mock
    private Yuv420Image image;

    @InjectMocks
    private EnrollmentPresenterForTest presenter;

    @SuppressLint("MissingSuperCall")
    @SuppressWarnings({"SameParameterValue", "unused"})
    private static class EnrollmentPresenterForTest extends EnrollmentPresenter {

        private boolean resetBiometricOperationCalled = false;
        private RuntimeException showWarningCalledWith = null;
        private MovementDirection captureImagePairCalledWithFirstParam = null;
        private MovementDirection captureImagePairCalledWithSecondParam = null;

        private EnrollmentPresenterForTest(Context ctx, LoggingHelper log, FacialRecognitionContract.View view,
                                           BackgroundHandler backgroundHandler, EnrollmentTokenProvider tokenProvider,
                                           BioIdWebserviceClient bioIdWebserviceClient) {
            super(ctx, log, view, backgroundHandler, tokenProvider, bioIdWebserviceClient);

            this.bwsToken = ENROLLMENT_TOKEN;
        }

        @Override
        protected void resetBiometricOperation() {
            resetBiometricOperationCalled = true;
        }

        @Override
        protected void showWarningOrError(RuntimeException e) {
            showWarningCalledWith = e;
        }

        @Override
        protected void captureImagePair(MovementDirection currentDirection, MovementDirection destinationDirection) {
            super.captureImagePair(currentDirection, destinationDirection);
            captureImagePairCalledWithFirstParam = currentDirection;
            captureImagePairCalledWithSecondParam = destinationDirection;
        }

        private EnrollmentToken getBwsToken() {
            return bwsToken;
        }

        private void setBwsToken(EnrollmentToken bwsToken) {
            this.bwsToken = bwsToken;
        }

        private void setSuccessfulUploads(int successfulUploads) {
            this.successfulUploads = successfulUploads;
        }

        private int getFailedOperations() {
            return this.failedOperations;
        }

        private void setFailedOperations(int failedOperations) {
            this.failedOperations = failedOperations;
        }
    }

    @Before
    public void setUp() throws Exception {
        when(tokenProvider.requestEnrollmentToken(ctx)).thenReturn(ENROLLMENT_TOKEN);
    }

    @Test
    public void startBiometricOperation_initialisationInfoIsShownWhileFetchingTheToken() throws Exception {
        presenter.startBiometricOperation();

        InOrder messageOrder = inOrder(view);
        messageOrder.verify(view).showInitialisationInfo();
        messageOrder.verify(view).hideMessages();
    }

    @Test
    public void startBiometricOperation_tokenIsFetchedAndSet() throws Exception {
        presenter.setBwsToken(null);
        presenter.startBiometricOperation();

        assertThat(presenter.getBwsToken(), is(ENROLLMENT_TOKEN));
    }

    @Test(expected = IllegalStateException.class)
    public void onFaceDetected_shouldNotBeCalled() {
        presenter.onFaceDetected();
    }

    @Test(expected = IllegalStateException.class)
    public void onNoFaceDetected_shouldNotBeCalled() {
        presenter.onNoFaceDetected();
    }

    @Test
    public void startBiometricOperation_promptForEnrollmentProcessExplanation() throws Exception {
        presenter.startBiometricOperation();

        verify(view).promptForEnrollmentProcessExplanation();
    }

    @Test
    public void startBiometricOperation_ifTokenRequestFailed_resetBiometricOperation() throws Exception {
        doThrow(RuntimeException.class).when(tokenProvider).requestEnrollmentToken(ctx);

        presenter.startBiometricOperation();

        assertThat(presenter.resetBiometricOperationCalled, is(true));
    }

    @Test
    public void startBiometricOperation_ifTokenRequestFailed_warningIsShown() throws Exception {
        RuntimeException e = new RuntimeException("token request failed");
        doThrow(e).when(tokenProvider).requestEnrollmentToken(ctx);

        presenter.startBiometricOperation();

        assertThat(presenter.showWarningCalledWith, is(e));
    }

    @Test
    public void promptForProcessExplanationAccepted_captureImagePairSessionTriggered() throws Exception {
        presenter.promptForProcessExplanationAccepted();

        assertThat(presenter.captureImagePairCalledWithFirstParam, is(MovementDirection.any));
        assertThat(presenter.captureImagePairCalledWithSecondParam, is(MovementDirection.any));
    }

    @Test
    public void promptForProcessExplanationRejected_doesNavigateBackWithoutSuccess() throws Exception {
        presenter.promptForProcessExplanationRejected();

        verify(view).navigateBack(false);
    }

    @Test
    public void promptToTurn90DegreesAccepted_captureImagePairSessionTriggered() throws Exception {
        presenter.promptToTurn90DegreesAccepted();

        assertThat(presenter.captureImagePairCalledWithFirstParam, is(MovementDirection.any));
        assertThat(presenter.captureImagePairCalledWithSecondParam, is(MovementDirection.any));
    }

    @Test
    public void promptToTurn90DegreesRejected_doesNavigateBackWithoutSuccess() throws Exception {
        presenter.promptToTurn90DegreesRejected();

        verify(view).navigateBack(false);
    }

    @Test
    public void onUploadSuccessful_enrollingInfoIsShown() throws Exception {
        presenter.setSuccessfulUploads(8);

        presenter.onUploadSuccessful();

        verify(view).showEnrollingInfo();
    }

    @Test
    public void onUploadSuccessful_resetBiometricOperation() throws Exception {
        presenter.setSuccessfulUploads(8);

        presenter.onUploadSuccessful();

        assertThat(presenter.resetBiometricOperationCalled, is(true));
    }

    @Test
    public void onUploadSuccessful_ifEnrollmentWasSuccessful_successWillBeShownBeforeNavigatingBack() throws Exception {
        presenter.setSuccessfulUploads(8);

        presenter.onUploadSuccessful();

        verify(view).showEnrollmentSuccess();
        verify(view).navigateBack(true);
    }

    @Test
    public void onUploadSuccessful_ifEnrollmentWasNotSuccessful_warningWillBeShown() throws Exception {
        presenter.setSuccessfulUploads(8);
        RuntimeException e = new RuntimeException("enrollment not successful");
        doThrow(e).when(bioIdWebserviceClient).enroll(ENROLLMENT_TOKEN);

        presenter.onUploadSuccessful();

        assertThat(presenter.showWarningCalledWith, is(e));
    }

    @Test
    public void onUploadSuccessful_ifEnrollmentWasNotSuccessful_operationWillBeRestarted() throws Exception {
        presenter.setSuccessfulUploads(8);
        RuntimeException e = new RuntimeException("enrollment not successful");
        doThrow(e).when(bioIdWebserviceClient).enroll(ENROLLMENT_TOKEN);

        presenter.onUploadSuccessful();

        assertThat(presenter.captureImagePairCalledWithFirstParam, is(MovementDirection.any));
        assertThat(presenter.captureImagePairCalledWithSecondParam, is(MovementDirection.any));
    }

    @Test
    public void onUploadSuccessful_ifEnrollmentWasNotSuccessful_failedOperationCounterIsIncremented() throws Exception {
        presenter.setSuccessfulUploads(8);
        RuntimeException e = new RuntimeException("enrollment not successful");
        doThrow(e).when(bioIdWebserviceClient).enroll(ENROLLMENT_TOKEN);

        presenter.onUploadSuccessful();

        assertThat(presenter.getFailedOperations(), is(1));
    }

    @Test
    public void onUploadSuccessful_ifEnrollmentWasNotSuccessfulForTheThirdTime_navigateBackWithoutSuccess() throws Exception {
        presenter.setSuccessfulUploads(8);
        RuntimeException e = new RuntimeException("enrollment not successful");
        doThrow(e).when(bioIdWebserviceClient).enroll(ENROLLMENT_TOKEN);
        presenter.setFailedOperations(2);

        presenter.onUploadSuccessful();

        verify(view).navigateBack(false);
    }

    @Test
    public void onUploadSuccessful_ifFirstImageOfPairWasUploaded_waitForSecondImageUploadToComplete() throws Exception {
        presenter.setSuccessfulUploads(3);  // one pair already uploaded + reference image of second pair

        presenter.onUploadSuccessful();

        verify(view, never()).promptToTurn90Degrees();
        verify(bioIdWebserviceClient, never()).enroll(any(EnrollmentToken.class));
    }

    @Test
    public void onUploadSuccessful_ifLessThanFourImagePairsAreUploaded_promptToTurn90Degrees() throws Exception {
        presenter.setSuccessfulUploads(6);  // three pairs already uploaded -> one missing

        presenter.onUploadSuccessful();

        verify(view).promptToTurn90Degrees();
        verify(bioIdWebserviceClient, never()).enroll(any(EnrollmentToken.class));
    }
}