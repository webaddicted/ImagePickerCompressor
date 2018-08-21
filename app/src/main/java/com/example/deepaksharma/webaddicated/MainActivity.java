package com.example.deepaksharma.webaddicated;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.deepaksharma.webaddicated.CompressImage.CompressImage;
import com.example.deepaksharma.webaddicated.permission.Permission.PermissionListener;
import com.example.deepaksharma.webaddicated.permission.Permission.Permissions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements PermissionListener {
    ImageView imgPick;
    private static final int REQUEST_CAMERA = 125;
    private static final int SELECT_FILE = 115;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imgPick = findViewById(R.id.img_pick);
    }

    public void onPickImage(View view) {
        List<String> permissionList = new ArrayList<>();
        permissionList.add(Manifest.permission.CAMERA);
        permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (Permissions.checkAndRequestPermission(this, permissionList, this))
            selectImage();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Permissions.checkResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onPermissionGranted(List<String> mCustomPermission) {
        selectImage();
    }

    @Override
    public void onPermissionDenied(List<String> mCustomPermission) {
        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
    }

    private void selectImage() {
        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Take Photo")) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, REQUEST_CAMERA);
                } else if (options[item].equals("Choose from Gallery")) {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_FILE);
                } else if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CAMERA:
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    imgPick.setImageBitmap(photo);
                    break;
                case SELECT_FILE:
                    File filePath = PathUtil.getPath(MainActivity.this, data.getData());
                    // compress image & store in cache if user not provide path in application Class
                    File file = CompressImage.compressImage(filePath.toString());
                    Bitmap thumbnail = (BitmapFactory.decodeFile(file.toString()));
//                    Uri selectedImage = data.getData();
//                    String[] filePath = {MediaStore.Images.Media.DATA};
//                    Cursor c = getContentResolver().query(selectedImage, filePath, null, null, null);
//                    c.moveToFirst();
//                    int columnIndex = c.getColumnIndex(filePath[0]);
//                    String picturePath = c.getString(columnIndex);
//                    c.close();
//                    Bitmap thumbnail = (BitmapFactory.decodeFile(picturePath));
                    imgPick.setImageBitmap(thumbnail);
                    break;
            }
        }
    }
}
