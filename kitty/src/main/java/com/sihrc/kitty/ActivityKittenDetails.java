package com.sihrc.kitty;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

/**
 * Created by chris on 12/22/13.
 */
public class ActivityKittenDetails extends FragmentActivity{
    /**
     * Database
     */
    HandlerDatabase db;
    Kitty curKitty;

    /**
     * Views
     */

    EditText category;
    ResizableImageView image;
    Button cancel;
    EditText status;
    EditText name;
    Button save;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kitten_details);

        //Get Intent Data
        db = new HandlerDatabase(this);
        db.open();
        curKitty = db.getKittyById(getIntent().getStringExtra("kittyId"));

        //Image
        image = (ResizableImageView)findViewById(R.id.activity_kitten_details_image);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setImageBitmap(BitmapFactory.decodeByteArray(curKitty.image, 0, curKitty.image.length));

        //Edit Text Fields
        name = (EditText)findViewById(R.id.activity_kitten_details_name);
        name.setText(curKitty.name);
        category = (EditText)findViewById(R.id.activity_kitten_details_category);
        category.setText(curKitty.category);
        status = (EditText)findViewById(R.id.activity_kitten_details_status);
        status.setText(curKitty.status);


        //Save and Cancel Buttons
        cancel = (Button)findViewById(R.id.activity_kitten_details_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!curKitty.name.equals(String.valueOf(name.getText()))
                        || !curKitty.category.equals(String.valueOf(category.getText()))
                        || !curKitty.status.equals(String.valueOf(status.getText()))){
                    new AlertDialog.Builder(ActivityKittenDetails.this)
                            .setTitle("Save changes?")
                            .setMessage("Do you want to save your changes before you leave?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    saveKitty();
                                    ActivityKittenDetails.this.finish();
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    ActivityKittenDetails.this.finish();
                                }
                            }).show();
                } else {
                    finish();
                }
            }
        });

        save = (Button)findViewById(R.id.activity_kitten_details_save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveKitty();
                ActivityKittenDetails.this.finish();
            }
        });
    }

    /**
     * Update Kitty
     */
    private void saveKitty(){
        curKitty.name = String.valueOf(name.getText());
        curKitty.category = String.valueOf(category.getText());
        curKitty.status = String.valueOf(status.getText());
        db.updateKitty(curKitty);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }
}
