package com.example.testlandmark;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private Button button;
    private Button button1;
    private ImageView imageView;
    private TextView textView;
    private Uri uri;
    private Bitmap photo;
    private Module module;
    @Override


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            // 导入模型
            module = Module.load(assetFilePath(this, "model.ptl"));
        } catch (IOException e) {
            e.printStackTrace();
            finish();
        }
        button = (Button) findViewById(R.id.choose);
        button1 = (Button) findViewById(R.id.button1);
        imageView = (ImageView) findViewById(R.id.imageView);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 从相册中区一张图片
                getImageFromAlbum();
            }
        });

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 未实现直接拍照 （需要用到CamreaX控件）
                //getImageFromCamera();

                // 调用模型
                UseModel();
            }
        });

    }

    protected void getImageFromAlbum(){
        // 相册中获取数据
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        // 传递2为 从相册中取照片
        startActivityForResult(intent, 2);
    }

    protected void getImageFromCamera(){
        // 拍照获取数据 （未使用CarmeraX, 未实现)
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            Intent getImageByCamera = new Intent("android.media.action.IMAGE_CAPTURE",null);
            startActivityForResult(getImageByCamera, 1);

//            Intent openCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//            Uri imageUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(),"image.jpg"));
//            //指定照片保存路径，image.jpg为一个临时文件，每次拍照后这个图片都会被替换
//            openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
//            startActivityForResult(openCameraIntent, 3);
        }
        else {

        }

    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2) {
            // 从相册返回的数据
            if (data != null) {
                // 得到图片的路径
                uri = data.getData();
                try {
                    photo = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                    photo = Bitmap.createScaledBitmap(photo, 512, 512, true);
                    // 转化成512 * 512 的 bitmap
                    Toast.makeText(this, "height and width are " + photo.getHeight() + " " + photo.getWidth(), Toast.LENGTH_SHORT).show();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                // 显示到界面上
                imageView.setImageBitmap(photo);
            }

        } else if (requestCode == 1) {
            // 未使用CameraX 得到的 bitmap 大小为 172 * 172 （格式太小）
            if (data != null) {
                uri = data.getData();
                if (uri == null) {
                    //用 bundle 获取数据
                    Bundle bundle = data.getExtras();
                    if (bundle != null) {
                        photo = (Bitmap) bundle.get("data"); //get bitmap
                        //photo = Bitmap.createScaledBitmap(photo, 512, 512, true);

                        Toast.makeText(this, "height and width are " + photo.getHeight() + " " + photo.getWidth(), Toast.LENGTH_SHORT).show();
//                        imageScale(photo, 512, 512);
                    } else {
                        Toast.makeText(getApplicationContext(), "err****", Toast.LENGTH_LONG).show();
                        return;
                    }
                } else {
                }
                imageView.setImageBitmap(photo);
            }
        }

    }


    public static String assetFilePath(Context context, String assetName) throws IOException {
        // 获取模型的绝对路径
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    public void UseModel (){
        // 将bitmap 转化为输入的张量
        Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(photo,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);

        // 得到输出
        Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
        float[] scores = outputTensor.getDataAsFloatArray();

        // 计算
        float maxScore = -Float.MAX_VALUE;
        int maxScoreIdx = -1;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > maxScore) {
                maxScore = scores[i];
                maxScoreIdx = i;
            }
        }
        String className = Tag.TAGS[maxScoreIdx];
        textView.setText(className);
    }

}