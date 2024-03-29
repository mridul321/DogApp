package com.example.android.dogsapp.view;


import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.palette.graphics.Palette;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.android.dogsapp.R;
import com.example.android.dogsapp.databinding.FragmentDetailBinding;
import com.example.android.dogsapp.databinding.SendSmsLayoutBinding;
import com.example.android.dogsapp.model.DogBreed;
import com.example.android.dogsapp.model.DogPallete;
import com.example.android.dogsapp.model.SmsInfo;
import com.example.android.dogsapp.util.Util;
import com.example.android.dogsapp.viewmodel.DetailViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import butterknife.BindView;
import butterknife.ButterKnife;


public class DetailFragment extends Fragment {

    private int dogUuid;
    private DetailViewModel viewModel;
    private FragmentDetailBinding binding;
    private Boolean sendSmsStarted = false;
    private DogBreed currentDog;




    public DetailFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        FragmentDetailBinding binding = DataBindingUtil.inflate(inflater,R.layout.fragment_detail,container,false);
        this.binding = binding;
        setHasOptionsMenu(true);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(getArguments() != null){
            dogUuid = DetailFragmentArgs.fromBundle(getArguments()).getDogUuid();
        }

        viewModel = ViewModelProviders.of(this).get(DetailViewModel.class);
        viewModel.fetch(dogUuid);

        observeViewModel();
    }

   public void observeViewModel(){
        viewModel.dogLiveData.observe(this, dogBreed -> {
            if(dogBreed != null && dogBreed instanceof DogBreed && getContext() != null){
                currentDog = dogBreed;
                  binding.setDog(dogBreed);
            }
            if(dogBreed.imageUrl != null){
                setUpBackgroundColor(dogBreed.imageUrl);
            }

        });
   }

   private void setUpBackgroundColor(String url){
       Glide.with(this)
               .asBitmap()
               .load(url)
               .into(new CustomTarget<Bitmap>() {
                   @Override
                   public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                       Palette.from(resource)
                               .generate(palette -> {
                                   int color = palette.getLightMutedSwatch().getRgb();
                                   DogPallete myPallete = new DogPallete(color);
                                   binding.setPalette(myPallete);
                               });
                   }

                   @Override
                   public void onLoadCleared(@Nullable Drawable placeholder) {

                   }
               });
   }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.detail_menu,menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_send_sms:{
                if(!sendSmsStarted){
                    sendSmsStarted = true;
                    ((MainActivity) getActivity()).checkSmsPermission();
                }
                break;
            }
            case R.id.action_share: {
               Intent intent = new Intent(Intent.ACTION_SEND);
               intent.setType("text/plain");
               intent.putExtra(Intent.EXTRA_SUBJECT,"Check out this dog breed");
               intent.putExtra(Intent.EXTRA_TEXT,currentDog.dogBreed + "bred for" + currentDog.bredFor);
               intent.putExtra(Intent.EXTRA_STREAM,currentDog.imageUrl);
               startActivity(Intent.createChooser(intent,"Share With"));
                break;
            }

        }
        return super.onOptionsItemSelected(item);
    }

    public void onPermissionsResult(boolean permisstionGranted) {
       if(isAdded() && sendSmsStarted && permisstionGranted){
           SmsInfo smsInfo = new SmsInfo("",currentDog.dogBreed + "bred for" + currentDog.bredFor,currentDog.imageUrl);

           SendSmsLayoutBinding dialogBinding = DataBindingUtil.inflate(
                   LayoutInflater.from(getContext()),
                   R.layout.send_sms_layout,
                   null,
                   false
           );

           new AlertDialog.Builder(getContext())
                   .setView(dialogBinding.getRoot())
                   .setPositiveButton("Send SMS",((dialog, which) ->{
                       if(!dialogBinding.smsDestination.getText().toString().isEmpty()){
                           smsInfo.to = dialogBinding.smsDestination.getText().toString();
                           sendSms(smsInfo);
                       }
                   })).setNegativeButton("Cancel",((dialog, which) -> {}))
                   .show();

           sendSmsStarted = false;
           dialogBinding.setSmsInfo(smsInfo);
       }
    }

    private void sendSms(SmsInfo smsInfo) {
        Intent intent = new Intent(getContext(),MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(getContext(),0,intent,0);

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(smsInfo.to,null,smsInfo.text,pi,null);

    }

}
