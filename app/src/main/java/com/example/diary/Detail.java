package com.example.diary;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.sql.Date;
import java.text.SimpleDateFormat;


import static com.example.diary.DatabaseHelper.TABLE_NAME;
import static com.example.diary.MainActivity.TAG_INSERT;
import static com.example.diary.MainActivity.TAG_UPDATE;
import static com.example.diary.MainActivity.dbHelper;
import static com.example.diary.MainActivity.getDbHelper;

public class Detail extends  AppCompatActivity {

    private SQLiteDatabase db;
    EditText title;
    EditText author;
    TextView time;
    EditText content;
    Button pictureChoice;
    ImageView picture;
    public DatabaseHelper deHelper= getDbHelper();
    private int tag;
    private int id;
    private static final int CHOICE_PHOTO=2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        title= findViewById(R.id.detail_title);
        author= findViewById(R.id.detail_author);
        time= findViewById(R.id.detail_time);
        content= findViewById(R.id.detail_content);
        pictureChoice=findViewById(R.id.detail_pictureChoice);
        picture=findViewById(R.id.detail_picture);
        title.setSelection(title.getText().length());
        author.setText("admin");
        //author.setSelection(author.getText().length());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        time.setText(simpleDateFormat.format(date));
        content.setSelection(content.getText().length());
        pictureChoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(Detail.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(Detail.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                } else {
                    openAlbum();
                }
            }
        });

        db= dbHelper.getWritableDatabase();
        Intent intent=getIntent();
        tag=intent.getIntExtra("TAG",-1);
        switch(tag){
            case TAG_INSERT:
                break;
            case TAG_UPDATE:
                id=intent.getIntExtra("ID",-1);
                    Cursor cursor=db.query(TABLE_NAME,null,"id=?",
                        new String[]{String.valueOf(id)},null,null,null);
                if(cursor.moveToFirst()){
                    String select_title=cursor.getString(cursor.getColumnIndex("title"));
                    String select_author=cursor.getString(cursor.getColumnIndex("author"));
                    String select_content=cursor.getString(cursor.getColumnIndex("content"));
                    title.setText(select_title);
                    author.setText(select_author);
                    content.setText(select_content);
                    byte[] in = cursor.getBlob(cursor.getColumnIndex("picture"));
                    Bitmap bitmap=BitmapFactory.decodeByteArray(in,0,in.length);
                    picture.setImageBitmap(bitmap);

                }
                break;
            default:
        }
    }

    //打开系统相册
    private  void openAlbum(){
        Intent  intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent,CHOICE_PHOTO);
    }

    //
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        switch (requestCode){
            case 1:
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    openAlbum();
                }else {
                    Toast.makeText(this,"You denied the permission",Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent  data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CHOICE_PHOTO:
                if (resultCode == RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= 19) {
                        handleImageOnKitKat(data);
                    } else {
                        handleImageBeforeKitKat(data);
                    }
                }
                break;
            default:
                break;
        }
    }

    //对图片进行解析
    @TargetApi(19)
    private void handleImageOnKitKat(Intent data){
        String imagePath = null;
        Uri uri =data.getData();
        if(DocumentsContract.isDocumentUri(this,uri)){
            String docId = DocumentsContract.getDocumentId(uri);
            if("com.android.providers.media.documents".equals(uri.getAuthority())){
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,selection);
            }else if("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
                }
            }else if("content".equalsIgnoreCase(uri.getScheme())){
                imagePath = getImagePath(uri,null);
            }else if("file".equalsIgnoreCase(uri.getScheme())){
                imagePath = uri.getPath();
            }
            displayImage(imagePath);
    }

    //图片显示前对图片进行解析
    private void handleImageBeforeKitKat(Intent data){
        Uri uri = data.getData();
        String imagePath = getImagePath(uri,null);
        displayImage(imagePath);
    }

    //获取图片的路径
    private String getImagePath(Uri uri, String selection){
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection,null, null);
        if(cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    //显示照片
    private void displayImage(String imagePath){
        Bitmap bitmap = null;
        if(imagePath != null){
            bitmap = BitmapFactory.decodeFile(imagePath);
            picture.setImageBitmap(bitmap);
        }else{
            Toast.makeText(this,"failed to get image", Toast.LENGTH_SHORT).show();
        }
    }

    //将menu中的actionbar添加进来
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    //设置“保存”或者“删除”按钮点击事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.save:
                if(tag==TAG_INSERT) {
                    ContentValues values = new ContentValues();
                    values.put("title", title.getText().toString());
                    values.put("author", author.getText().toString());
                    values.put("content", content.getText().toString());
                    final ByteArrayOutputStream os = new ByteArrayOutputStream();
                    Bitmap bitmap = ((BitmapDrawable)picture.getDrawable()).getBitmap();
                    bitmap.compress(Bitmap.CompressFormat.PNG,100,os);
                    values.put("picture",os.toByteArray());
                    db.insert(TABLE_NAME, null, values);
                    values.clear();
                    Toast.makeText(this, "Save", Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                }else if(tag==TAG_UPDATE){
                    //修改title、content和picture
                    String update_title=title.getText().toString();
                    String update_author=author.getText().toString();
                    String update_content=content.getText().toString();
                    ContentValues values=new ContentValues();
                    values.put("title",update_title);
                    values.put("author",update_author);
                    values.put("content",update_content);
                    final ByteArrayOutputStream os = new ByteArrayOutputStream();
                    Bitmap bitmap = ((BitmapDrawable)picture.getDrawable()).getBitmap();
                    bitmap.compress(Bitmap.CompressFormat.PNG,100,os);
                    values.put("picture",os.toByteArray());
                    db.update(TABLE_NAME,values,"id=?",new String[]{String.valueOf(id)});
                    finish();
                    break;
                }
            case R.id.delete:
                if(tag==TAG_UPDATE) {
                    db.delete(TABLE_NAME,"id=?",new String[]{String.valueOf(id)});
                }
                Toast.makeText(this,"Delete",Toast.LENGTH_SHORT).show();
                finish();
                break;
            case R.id.goBack:
                finish();
                break;
            default:
        }
        return true;
    }
}
