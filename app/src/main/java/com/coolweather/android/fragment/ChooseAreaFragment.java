package com.coolweather.android.fragment;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.android.R;
import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.jetbrains.annotations.NotNull;
import org.litepal.LitePal;
import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {

    public static final int LEVEL_PROVINCE = 0;

    public static final int LEVEL_CITY = 1;

    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;

   private TextView titleText;

   private Button backButton;

   private ListView listView;

   private ArrayAdapter<String> adapter;


   private List<String>dataList = new ArrayList<>();

   //省列表
    private List<Province>provinceList;

    //市列表
    private List<City>cityList;

    //县列表
    private List<County>countyList;

    //选中的省份
    private Province selectProvince;

    //选中的市
    private City selectCity;

    //当前选中的级别
    private int currentLevel;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area,container,false);
        titleText = view.findViewById(R.id.title_text);
        backButton = view.findViewById(R.id.back_button);
        listView = view.findViewById(R.id.list_view);

        Log.v("dataList",dataList.size()+"");

        adapter = new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);

        return view;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if(currentLevel == LEVEL_PROVINCE){
                    selectProvince = provinceList.get(position);
                    queryCites();
                }else if (currentLevel == LEVEL_CITY){
                    selectCity = cityList.get(position);
                    queryCounties();
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTY){
                    queryCites();
                }else {
                    queryProvinces();
                }
            }
        });

        queryProvinces();
    }



    /**
     * 查询全国的所有省，优先从数据库查询，如果没有再去服务器
     */
    private void queryProvinces(){

        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList = LitePal.findAll(Province.class);
        if (provinceList.size()>0){
            dataList.clear();;
            for (Province province : provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        }else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }


    /**
     * 查询所选省的所有市，优先从数据库查询，如果没有再去服务器
     */
    private void queryCites(){
        titleText.setText(selectProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = LitePal.where("provinceid = ?" ,String.valueOf(selectProvince.getId())).
                find(City.class);
        if (cityList.size() > 0){
            dataList.clear();
            for (City city : cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        }else {
            int provinceCode = selectProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/"+provinceCode;
            queryFromServer(address,"city");
        }
               // DataSupport.where()
    }

    /**
     * 查询所选市的所有县，优先从数据库查询，如果没有再去服务器
     */
    private void queryCounties(){
        titleText.setText(selectCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = LitePal.where("cityid = ?" ,String.valueOf(selectCity.getId())).
                find(County.class);
        if (countyList.size() > 0){
            for (County county : countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }else {
            int provinceCode = selectProvince.getProvinceCode();
            int cityCode = selectCity.getCityCode();
            String address = "http://guolin.tech/api/china/"+provinceCode +
                    provinceCode + "/"+cityCode;
            queryFromServer(address,"county");
        }

    }

    /**
     * 根据传入的地址和类型从服务器上查询省市县数据
     * @param address
     * @param type
     */

  private void   queryFromServer(String address,final String type){
    showprogressDialog();
      HttpUtil.sendOkHttpRequest(address, new Callback() {
          @Override
          public void onFailure(@NotNull Call call, @NotNull IOException e) {
              Log.v("JSON ==","ssssss"+e.getMessage());
             // Log.v("eeee ==","eee"+ e.getLocalizedMessage());
              e.printStackTrace();



              getActivity().runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                      closeProgressDialog();
                      Toast.makeText(getContext(),"加载失败",Toast.LENGTH_LONG).show();
                  }
              });
          }

          @Override
          public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {

              String responseText = response.body().string();

              Log.v("JSON ==",responseText);
              boolean result = false;
              if ("province".equals(type)){
                  result = Utility.handleProvinceResponse(responseText);
                  Log.v("province ==result",result+"");
              }else if ("city".equals(type)){
                  result = Utility.handleCityResponse(responseText,selectProvince.getId());
              }else if ("county".equals(type)){
                  result = Utility.handleCountyResponse(responseText,selectCity.getId());
              }
              Log.v("result ==",result+"");
              if (result){
                  getActivity().runOnUiThread(new Runnable() {
                      @Override
                      public void run() {
                          closeProgressDialog();
                          if ("province".equals(type)){
                              queryProvinces();
                          }else if ("city".equals(type)){
                              queryCites();
                          }else if ("county".equals(type)){
                              queryCounties();
                          }
                      }
                  });
              }


          }
      });

    }

    private void showprogressDialog(){

      if (progressDialog == null){
          progressDialog = new ProgressDialog(getActivity());
          progressDialog.setMessage("正在加载……");
          progressDialog.setCanceledOnTouchOutside(false);

      }
      progressDialog.show();
    }
   private void closeProgressDialog(){
       if (progressDialog != null){
           progressDialog.dismiss();
       }
    }
}
