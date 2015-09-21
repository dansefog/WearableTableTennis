package com.example.jhon.wearabletabletennis;

import java.lang.Float;import java.util.ArrayList;
import java.util.Collections;

public class SensorFilter {

    private ArrayList<Float> mFirst     = new ArrayList<Float>();
    private ArrayList<Float> mSecond    = new ArrayList<Float>();
    private ArrayList<Float> mThrad     = new ArrayList<Float>();

    private final float GAIN = 0.5f;

    public int sampleCount=1;//サンプリング数
    public int sampleNum = 0;//サンプリングした値の使用値のインデックス
    //サンプリング数1、インデックス0にすることでメディアンフィルタをオフにできる

    private float[] mParam = new float[3];//フィルタをかけた後の値

    private boolean mSampleEnable=false;//規定のサンプリング数に達したか

    /**
     * フィルタをかけた値を返す
     * @return
     */
    public float[] getParam()
    {
        return mParam;
    }

    /**
     * サンプリングする値を追加
     * @param sample 要素３のfloatの配列オブジェクト
     */
    void addSample(float[] sample)
    {
        //サンプリング数の追加
        mFirst.add(sample[0]);
        mSecond.add(sample[1]);
        mThrad.add(sample[2]);

        //必要なサンプリング数に達したら
        if(mFirst.size() == sampleCount)
        {
            //メディアンフィルタ(サンプリング数をソートして中央値を使用)かけて値を取得
            //さらにローパスフィルタをかける

            ArrayList<Float> lst = (ArrayList<Float>) mFirst.clone();
            Collections.sort(lst);
            mParam[0] =(mParam[0]*GAIN) + lst.get(sampleNum)*(1-GAIN);

            lst = (ArrayList<Float>) mSecond.clone();
            Collections.sort(lst);
            mParam[1] = (mParam[1]*GAIN) +lst.get(sampleNum)*(1-GAIN);

            lst = (ArrayList<Float>) mThrad.clone();
            Collections.sort(lst);
            mParam[2] = (mParam[2]*GAIN) +lst.get(sampleNum)*(1-GAIN);

            mSampleEnable = true;

            //一番最初の値を削除
            mFirst.remove(0);
            mSecond.remove(0);
            mThrad.remove(0);
        }
    }

    /**
     * 規定のサンプリング数に達したか
     * @return
     */
    public boolean isSampleEnable()
    {
        return mSampleEnable;
    }

}
