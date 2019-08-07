package nytech.com.travelmantics;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.Objects;

public class DealActivity extends AppCompatActivity {

    FirebaseDatabase mFirebaseDatabase;
    DatabaseReference mDatabaseReference;
    TravelDeal mDeal;
    EditText etTitle,etDescription,etPrice;
    private static final int PICTURE_RESULT = 42;
    ImageView dealImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FirebaseUtil.openFbReference("TravelDeals", this);
        mFirebaseDatabase = FirebaseUtil.mFirebaseDatabase;
        mDatabaseReference = FirebaseUtil.mDatabaseReference;

        etTitle = findViewById(R.id.et_title);
        etPrice = findViewById(R.id.et_price);
        etDescription = findViewById(R.id.et_description);
        dealImage = findViewById(R.id.img_deal);

        Intent intent = getIntent();
        TravelDeal deal = (TravelDeal) intent.getSerializableExtra("deal");
        if(deal == null){
            deal = new TravelDeal();
        }
        this.mDeal = deal;
        etTitle.setText(mDeal.getTitle());
        etDescription.setText(mDeal.getDescription());
        etPrice.setText(mDeal.getPrice());
        showImage(mDeal.getImageUrl());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem saveMenu = menu.findItem(R.id.action_save);
        MenuItem deleteMenu = menu.findItem(R.id.action_delete);
        MenuItem uploadMenu = menu.findItem(R.id.action_upload);
        if(FirebaseUtil.isAdmin){
            saveMenu.setVisible(true);
            deleteMenu.setVisible(true);
            uploadMenu.setVisible(true);
        }else{
            saveMenu.setVisible(false);
            deleteMenu.setVisible(false);
            uploadMenu.setVisible(false);
            etTitle.setEnabled(false);
            etDescription.setEnabled(false);
            etPrice.setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save) {
            if(saveDeal()){
                 clean();
            }
            return true;
        }else if(id == R.id.action_delete){
            delete();
            Toast.makeText(getApplicationContext(),"Successfully Deleted",Toast.LENGTH_LONG).show();
            backToList();
            return true;
        }else if(id == R.id.action_upload){
            uploadImage();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("IN ONACTIVITY RESULT ", requestCode+","+resultCode);
        if(requestCode == PICTURE_RESULT && resultCode == RESULT_OK){
            if (data != null) {
                Log.d("IS DATA NULL ", String.valueOf(data));
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    Log.d("IS IMAGE URL NULL ", String.valueOf(imageUri));
                    final StorageReference ref = FirebaseUtil.mStorageReference.child(Objects.requireNonNull(imageUri.getLastPathSegment()));
                    //add file on Firebase and got Download Link
                    ref.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //here
                            Task<Uri> urlTask = taskSnapshot.getStorage().getDownloadUrl();
                            String pictureName = taskSnapshot.getStorage().getPath();
                            while (!urlTask.isSuccessful());
                            Uri downloadUrl = urlTask.getResult();

                            final String url = String.valueOf(downloadUrl);

                            mDeal.setImageUrl(url);
                            mDeal.setImageName(pictureName);
                            showImage(url);
                        }
                    });
                }
            }
        }
    }

    private void uploadImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY,true);
        startActivityForResult(Intent.createChooser(intent,"Insert Picture"), PICTURE_RESULT);
    }

    private void backToList() {
        Intent intent = new Intent(this, ListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void clean()
    {
        etTitle.setText("");
        etDescription.setText("");
        etPrice.setText("");
        etTitle.requestFocus();
    }

    private boolean saveDeal()
    {
        String title = etTitle.getText().toString();
        String price = etPrice.getText().toString();
        String description = (etDescription.getText().toString());

        if(!title.equals("") && !price.equals("") && !description.equals("")){

            mDeal.setTitle(title);
            mDeal.setDescription(description);
            mDeal.setPrice(price);

            Log.d("This is the deal ",mDeal.toString());

            if(mDeal.getId() == null){
                mDatabaseReference.push().setValue(mDeal);
                Toast.makeText(this,"Travel Data Saved Successfully",Toast.LENGTH_LONG).show();
                return true;
            }else{
                mDatabaseReference.child(mDeal.getId()).setValue(mDeal);
                Toast.makeText(this,"Travel Data Updated successfully",Toast.LENGTH_LONG).show();
                backToList();
                return false;
            }
        }else{

            Toast.makeText(this,"All Fields are Required",Toast.LENGTH_LONG).show();
            return false;
        }
    }


    private void delete(){
        if(mDeal == null){
            Toast.makeText(getApplicationContext(),"Please create a deal before deleting",Toast.LENGTH_LONG).show();
            return;
        }
        mDatabaseReference.child(mDeal.getId()).removeValue();
        if(mDeal.getImageName() != null && mDeal.getImageName().isEmpty() == false){
            StorageReference picRef = FirebaseUtil.mStorageReference.child(mDeal.getImageName());
            picRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                }
            });
        }
    }

    private void showImage(String url){
        if(url != null && !url.isEmpty()){
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            Picasso.with(this)
                    .load(url)
                    .resize(width,width*2/3)
                    .centerCrop()
                    .into(dealImage);
        }
    }

}
