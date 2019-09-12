package com.example.android.dogsapp.viewmodel;

import android.app.Application;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.room.Database;

import com.example.android.dogsapp.model.DogBreed;
import com.example.android.dogsapp.model.DogDao;
import com.example.android.dogsapp.model.DogDatabase;
import com.example.android.dogsapp.model.DogsApiService;
import com.example.android.dogsapp.util.NotificationHelper;
import com.example.android.dogsapp.util.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class ListViewModel extends AndroidViewModel {

    public MutableLiveData<List<DogBreed>> dogs = new MutableLiveData<List<DogBreed>>();
    public MutableLiveData<Boolean> dogError = new MutableLiveData<Boolean>();
    public MutableLiveData<Boolean> loading = new MutableLiveData<Boolean>();

    private DogsApiService dogsService = new DogsApiService();
    private CompositeDisposable disposable = new CompositeDisposable();

    private AsyncTask<List<DogBreed>,Void,List<DogBreed>> insertTask;
    private AsyncTask<Void,Void,List<DogBreed>> retrieveTask;

    private SharedPreferencesHelper prefHelper = SharedPreferencesHelper.getInstance(getApplication());
    private long refreshTime = 5 * 60 * 1000 * 1000 * 1000L;

    public ListViewModel(@NonNull Application application) {
        super(application);
    }

    public void refresh(){
        checkCacheDuration();
        long updateTime = prefHelper.getUpdateTime();
        long currentTime = System.nanoTime();
        if(updateTime != 0 && updateTime - currentTime < refreshTime){
            fetchFromDatabase();
        }else {
            fetchFromRemote();
        }

    }

    public void RefreshByPassCache(){
        NotificationHelper.getInstance(getApplication()).createNotification();
        fetchFromRemote();
    }

    private void checkCacheDuration(){

        String cachePreference = prefHelper.getCacheDuration();

        if(!cachePreference.equals("")){
            try{
                int cacheprefInt = Integer.parseInt(cachePreference);
                refreshTime = cacheprefInt * 1000 * 1000 * 1000L;

            }catch (NumberFormatException e){
                e.printStackTrace();
            }
        }

    }
    private void fetchFromDatabase(){
        loading.setValue(true);
        retrieveTask = new RetrieveDogTask();
        retrieveTask.execute();
    }
    private void fetchFromRemote(){

        loading.setValue(true);

        disposable.add(
          dogsService.getDogs()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSingleObserver<List<DogBreed>>(){
                    @Override
                    public void onSuccess(List<DogBreed> dogBreeds) {
                      insertTask = new InsertDogsTask();
                      insertTask.execute(dogBreeds);
                        Toast.makeText(getApplication(), "Dogs retrieved from endpoint", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable e) {
                      dogError.setValue(true);
                      loading.setValue(false);
                      e.printStackTrace();
                    }
                })
        );

    }

    private void dogsRetrieved(List<DogBreed> dogList){
        dogs.setValue(dogList);
        dogError.setValue(false);
        loading.setValue(false);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposable.clear();

        if(insertTask != null){
            insertTask.cancel(true);
            insertTask = null;
        }
        if(retrieveTask != null){
            retrieveTask.cancel(true);
            retrieveTask = null;
        }
    }

    private class InsertDogsTask extends AsyncTask<List<DogBreed>,Void,List<DogBreed>>{

        @Override
        protected List<DogBreed> doInBackground(List<DogBreed>... lists) {
            List<DogBreed> list = lists[0];
            DogDao dao = DogDatabase.getInstance(getApplication()).dogDao();
            dao.deleteAllDog();

            ArrayList<DogBreed> newList = new ArrayList<>(list);
            List<Long> result = dao.insertAll(newList.toArray(new DogBreed[0]));

            int i=0;
            while (i< list.size()){
                list.get(i).uuid = result.get(i).intValue();
                ++i;

            }
            return list;
        }

        @Override
        protected void onPostExecute(List<DogBreed> dogBreeds) {
            dogsRetrieved(dogBreeds);
            prefHelper.saveUpdatTime(System.nanoTime());
        }
    }

    private class RetrieveDogTask extends AsyncTask<Void,Void,List<DogBreed>>{

        @Override
        protected List<DogBreed> doInBackground(Void... voids) {
            return DogDatabase.getInstance(getApplication()).dogDao().getAllDogs();
        }

        @Override
        protected void onPostExecute(List<DogBreed> dogBreeds) {
            dogsRetrieved(dogBreeds);
            Toast.makeText(getApplication(), "Dogs retrieved from database", Toast.LENGTH_SHORT).show();
        }
    }
}
