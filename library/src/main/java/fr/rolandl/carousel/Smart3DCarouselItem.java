package fr.rolandl.carousel;

import android.content.Context;
import android.graphics.Matrix;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

/**
 * @author Ludovic Roland
 * @since 2014.01.03
 */
public abstract class Smart3DCarouselItem<T1, T2>
    extends FrameLayout
    implements Comparable<Smart3DCarouselItem<?, ?>>
{

  private int index;

  private float currentAngle;

  private float itemX;

  private float itemY;

  private float itemZ;

  private boolean drawn;

  private Matrix mCIMatrix;

  public Smart3DCarouselItem(Context context, int layoutId)
  {
    super(context);
    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    this.setLayoutParams(params);

    LayoutInflater inflater = LayoutInflater.from(context);
    final View view = inflater.inflate(layoutId, this, true);

    extractView(view);
  }

  public abstract void extractView(View view);

  public void setIndex(int index)
  {
    this.index = index;
  }

  public int getIndex()
  {
    return index;
  }

  public void setCurrentAngle(float currentAngle)
  {
    this.currentAngle = currentAngle;
  }

  public float getCurrentAngle()
  {
    return currentAngle;
  }

  @Override
  public int compareTo(Smart3DCarouselItem<?, ?> another)
  {
    return (int) (another.itemZ - this.itemZ);
  }

  public void setItemX(float x)
  {
    this.itemX = x;
  }

  public float getItemX()
  {
    return itemX;
  }

  public void setItemY(float y)
  {
    this.itemY = y;
  }

  public float getItemY()
  {
    return itemY;
  }

  public void setItemZ(float z)
  {
    this.itemZ = z;
  }

  public float getItemZ()
  {
    return itemZ;
  }

  public void setDrawn(boolean drawn)
  {
    this.drawn = drawn;
  }

  public boolean isDrawn()
  {
    return drawn;
  }

  // public void setImageBitmap(Bitmap bitmap)
  // {
  // mImage.setImageBitmap(bitmap);
  //
  // }

  public void update2(T1 arg0, T2 arg1)
  {
    update(arg0, arg1);
  }

  public abstract void update(T1 arg0, T2 arg1);

  Matrix getCIMatrix()
  {
    return mCIMatrix;
  }

  void setCIMatrix(Matrix mMatrix)
  {
    this.mCIMatrix = mMatrix;
  }

}
