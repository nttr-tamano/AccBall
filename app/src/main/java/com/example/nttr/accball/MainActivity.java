package com.example.nttr.accball;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainActivity extends AppCompatActivity implements SensorEventListener,SurfaceHolder.Callback {

    SensorManager mSensorManager;
    Sensor mAccSensor;

    SurfaceHolder mHolder;
    int mSurfaceWidth;      // サーフェスビューの幅
    int mSurfaceHeight;     // サーフェスビューの高さ

    static final float RADIUS = 105.0f;     // ボールの半径
    static final float COEF = 1000.0f;      // ボールの画面上の移動量を調整するための係数
    static final int DIA = (int) RADIUS * 2;    // ボールの直径。画面描画用のため整数

    float mBallX;   // ボールの現在のx座業
    float mBallY;   // ボールの現在のy座標
    float mVX;      // ボールのx軸方向への速度
    float mVY;      // ボールのy軸方向への速度

    long mT0;       // 前回、センサーから加速度を取得した時間

    Bitmap mBallBitmap; // ボールの画像

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 端末の表示を縦に固定し、自動回転禁止
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mHolder = surfaceView.getHolder();

        mHolder.addCallback(this);  // thisはこのアクティビティ自身

        // サーフェスビューを透明にする
        mHolder.setFormat(PixelFormat.TRANSLUCENT);
        surfaceView.setZOrderOnTop(true);

        // ボールの画像を用意する
        // drawableに置いたリソース画像を読み込む
        Bitmap ball = BitmapFactory.decodeResource(getResources(),R.drawable.ball);
        mBallBitmap = Bitmap.createScaledBitmap(ball, DIA, DIA, false);

    }

    // 加速度センサーの値に変化があったときに呼ばれる
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//            Log.d("加速度", "x="+ event.values[0]
//                + " y=" + event.values[1]
//                + " z=" + event.values[2]);

            // 加速度
            float ax = -event.values[0]; // xは画面の描画方向と逆になるらしい
            float ay =  event.values[1];

            // 時間を求める
            if (mT0 == 0) {
                mT0 = event.timestamp;
                return;
            }
            float t = event.timestamp - mT0;
            mT0 = event.timestamp;
            t = t / 1000000000.0f;  // ナノ秒(ns)を秒(s)に単位変換

            // 移動距離を求める d = v0 * t + 1/2 * a * t^2
            float dx = (mVX * t) + (ax * t * t / 2.0f);
            float dy = (mVY * t) + (ay * t * t / 2.0f);

            // 移動距離から、ボールの今の位置を更新
            mBallX = mBallX + dx * COEF;
            mBallY = mBallY + dy * COEF;

            // 現在のボールの移動速度を更新 v = v0 + a * t
            mVX = mVX + (ax * t);
            mVY = mVY + (ay * t);

            // ボールが画面の外に出ないようにする処理
            // ボールの半径(RADIUS)の分だけ幅を持たせて反射する
            // 速度の方向を反転させて、少し小さくする( / 1.5f )

            // 左端 で 速度が左方向
            if (mBallX - RADIUS < 0 && mVX < 0) {
                mVX = -mVX / 1.5f;
                mBallX = RADIUS;

            // 右橋 で 速度が右方向
            } else if (mBallX + RADIUS > mSurfaceWidth && mVX > 0) {
                mVX = -mVX / 1.5f;
                mBallX = mSurfaceWidth - RADIUS;

            }

            // 上端 で 速度が上方向
            if (mBallY - RADIUS < 0 && mVY < 0) {
                mVY = -mVY / 1.5f;
                mBallY = RADIUS;

            // 下端 で 速度が下方向
            } else if (mBallY + RADIUS > mSurfaceHeight && mVY > 0) {
                mVY = -mVY / 1.5f;
                mBallY = mSurfaceHeight - RADIUS;

            }

            // 加速度から算出したボールの現在位置で、ボールをキャンバスに描画し直す
            drawCanvas();

        }

    }

    private void drawCanvas() {
        // 画面にボールを表示する処理

//        Paint paint = new Paint(); // Canvasに描画するための筆
//        paint.setStrokeWidth(20);
//        paint.setColor(Color.BLUE);

        Canvas c = mHolder.lockCanvas();
        // 前回の描画をクリア
        c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        Paint paint = new Paint();
        // 現在の座標がボールの真ん中になるように、描画開始位置（左上）に半径分だけずらしている
        c.drawBitmap(mBallBitmap, mBallX - RADIUS, mBallY - RADIUS, paint);

        mHolder.unlockCanvasAndPost(c);

    }

    // 加速度センサーの精度が変更されたときに呼ばれる
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // 画面が表示されたときに呼ばれるメソッド
    @Override
    protected void onResume() {
        super.onResume();

        // センサーの管理をサーフェスに連動させるため、
        // センサー作成はサーフェス生成時へ変更
//        mSensorManager.registerListener(this,mAccSensor,SensorManager.SENSOR_DELAY_GAME);
    }

    // 画面が閉じられた？ときに呼ばれるメソッド
    @Override
    protected void onPause() {
        super.onPause();

        // センサー削除はサーフェス削除時へ変更
//        mSensorManager.unregisterListener(this);
    }

    // サーフェスが作成されたとき
    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        mSensorManager.registerListener(this,mAccSensor,SensorManager.SENSOR_DELAY_GAME);
    }

    // サーフェスに変更があったとき
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mSurfaceHeight = height;
        mSurfaceWidth = width;

        // ボールの最初の位置を指定する
        mBallX = mSurfaceWidth / 2;
        mBallY = mSurfaceHeight / 2;

        // 最初の速度、最初の時間を初期化
        mVX = 0;
        mVY = 0;
        mT0 = 0;

    }

    // サーフェスが削除されるとき
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        mSensorManager.unregisterListener(this);
    }
}
