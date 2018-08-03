package com.bioid.authenticator.facialrecognition.photoverify;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bioid.authenticator.R;

import java.io.IOException;

public class PhotoVerifyBaseFragment extends Fragment {
    private static final int CAMERA_PIC_REQUEST = 968;
    private static final int PICK_IMAGE = 228;

    View fragmentView;

    public PhotoVerifyBaseFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_photo_verify_base, container, false);

        fragmentView.findViewById(R.id.photo_verify_camera_btn).setOnClickListener(v -> {
            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
        });

        fragmentView.findViewById(R.id.photo_verify_gallery_btn).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
        });

        return fragmentView;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode){
            case CAMERA_PIC_REQUEST:
                Bitmap imageFromCamera = (Bitmap) data.getExtras().get("data");
                this.endFragment(imageFromCamera);
                break;
            case PICK_IMAGE:
                Uri uri = data.getData();
                try {
                    Bitmap imageFromGallery = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), uri);
                    this.endFragment(imageFromGallery);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                throw new IllegalStateException("Unknown activity result code");
        }
    }

    private void endFragment(Bitmap imageFromCamera) {
        ((PhotoVerifyActivity) getActivity()).endIdPhotoSession(imageFromCamera);
    }
}