package fr.rolandl.carousel;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsSpinner;
import android.widget.SpinnerAdapter;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Igor Kushnarev, Ludovic Roland
 * @since 2014.12.19
 */
//Inspired by http://www.codeproject.com/Articles/146145/Android-D-Carousel
public abstract class CarouselSpinner
    extends CarouselBaseAdapter<SpinnerAdapter>
{

  private static class SavedState
      extends BaseSavedState
  {

    public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>()
    {
      @Override
      public SavedState createFromParcel(Parcel in)
      {
        return new SavedState(in);
      }

      @Override
      public SavedState[] newArray(int size)
      {
        return new SavedState[size];
      }

    };

    private long selectedId;

    private int position;

    /**
     * Constructor called from {@link AbsSpinner#onSaveInstanceState()}
     */
    SavedState(Parcelable superState)
    {
      super(superState);
    }

    /**
     * Constructor called from {@link #CREATOR}
     */
    private SavedState(Parcel in)
    {
      super(in);
      selectedId = in.readLong();
      position = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
      super.writeToParcel(out, flags);
      out.writeLong(selectedId);
      out.writeInt(position);
    }

    @Override
    public String toString()
    {
      return "AbsSpinner.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " selectedId=" + selectedId + " position=" + position + "}";
    }

  }

  protected class RecycleBin
  {

    private final SparseArray<View> scrapHeap = new SparseArray<>();

    public void put(int position, View v)
    {
      scrapHeap.put(position, v);
    }

    public View get(int position)
    {
      final View result = scrapHeap.get(position);

      if (result != null)
      {
        scrapHeap.delete(position);
      }

      return result;
    }

    public void clear()
    {
      final SparseArray<View> scrapHeap = this.scrapHeap;
      final int count = scrapHeap.size();

      for (int i = 0; i < count; i++)
      {
        final View view = scrapHeap.valueAt(i);

        if (view != null)
        {
          removeDetachedView(view, true);
        }
      }

      scrapHeap.clear();
    }

  }

  protected SpinnerAdapter adapter;

  private int heightMeasureSpec;

  private int widthMeasureSpec;

  private boolean blockLayoutRequests;

  private int selectionLeftPadding = 0;

  private int selectionTopPadding = 0;

  private int selectionRightPadding = 0;

  private int selectionBottomPadding = 0;

  private DataSetObserver dataSetObserver;

  protected final Rect spinnerPadding = new Rect();

  protected final RecycleBin recycler = new RecycleBin();

  public CarouselSpinner(Context context)
  {
    super(context);
    initCarouselSpinner();
  }

  public CarouselSpinner(Context context, AttributeSet attrs)
  {
    this(context, attrs, 0);
  }

  public CarouselSpinner(Context context, AttributeSet attrs, int defStyle)
  {
    super(context, attrs, defStyle);
    initCarouselSpinner();
  }

  /**
   * Common code for different constructor flavors
   */
  private void initCarouselSpinner()
  {
    setFocusable(true);
    setWillNotDraw(false);
  }

  /**
   * Jump directly to a specific item in the adapter data.
   */
  public void setSelection(int position, boolean animate)
  {
    // Animate only if requested position is already on screen somewhere
    //boolean shouldAnimate = animate && firstPosition <= position && position <= firstPosition + getChildCount() - 1;
    setSelectionInt(position, animate);
  }

  /**
   * Makes the item at the supplied position selected.
   *
   * @param position Position to select
   * @param animate  Should the transition be animated
   */
  private void setSelectionInt(int position, boolean animate)
  {
    if (position != oldSelectedPosition)
    {
      blockLayoutRequests = true;
      final int delta = position - selectedPosition;
      setNextSelectedPositionInt(position);
      layout(delta, animate);
      blockLayoutRequests = false;
    }
  }

  /**
   * Clear out all children from the list
   */
  void resetList()
  {
    dataChanged = false;
    needSync = false;

    removeAllViewsInLayout();
    oldSelectedPosition = CarouselBaseAdapter.INVALID_POSITION;
    oldSelectedRowId = CarouselBaseAdapter.INVALID_ROW_ID;

    setSelectedPositionInt(CarouselBaseAdapter.INVALID_POSITION);
    setNextSelectedPositionInt(CarouselBaseAdapter.INVALID_POSITION);
    invalidate();
  }

  private int getChildHeight(View child)
  {
    return child.getMeasuredHeight();
  }

  private int getChildWidth(View child)
  {
    return child.getMeasuredWidth();
  }

  protected void recycleAllViews()
  {
    final int childCount = getChildCount();
    final CarouselSpinner.RecycleBin recycleBin = recycler;
    final int position = firstPosition;

    // All views go in recycler
    for (int i = 0; i < childCount; i++)
    {
      View v = getChildAt(i);
      int index = position + i;
      recycleBin.put(index, v);
    }
  }

  /**
   * Maps a point to a position in the list.
   *
   * @param x X in local coordinate
   * @param y Y in local coordinate
   * @return The position of the item which contains the specified point, or {@link #INVALID_POSITION} if the point does not intersect an item.
   */
  public int pointToPosition(int x, int y)
  {

    final ArrayList<CarouselItem<?>> fitting = new ArrayList<>();

    if (adapter != null)
    {
      for (int i = 0; i < adapter.getCount(); i++)
      {
        final CarouselItem<?> item = (CarouselItem<?>) getChildAt(i);
        final Matrix mm = item.getCIMatrix();
        final float[] pts = new float[3];

        pts[0] = item.getLeft();
        pts[1] = item.getTop();
        pts[2] = 0;

        mm.mapPoints(pts);

        final int mappedLeft = (int) pts[0];
        final int mappedTop = (int) pts[1];

        pts[0] = item.getRight();
        pts[1] = item.getBottom();
        pts[2] = 0;

        mm.mapPoints(pts);

        final int mappedRight = (int) pts[0];
        final int mappedBottom = (int) pts[1];

        if (mappedLeft < x && mappedRight > x & mappedTop < y && mappedBottom > y)
        {
          fitting.add(item);
        }
      }
    }

    Collections.sort(fitting);

    if (fitting.size() != 0)
    {
      return fitting.get(0).getIndex();
    }
    else
    {
      return selectedPosition;
    }
  }

  @Override
  public Parcelable onSaveInstanceState()
  {
    final Parcelable superState = super.onSaveInstanceState();
    final SavedState ss = new SavedState(superState);
    ss.selectedId = getSelectedItemId();

    if (ss.selectedId >= 0)
    {
      ss.position = getSelectedItemPosition();
    }
    else
    {
      ss.position = CarouselBaseAdapter.INVALID_POSITION;
    }

    return ss;
  }

  @Override
  public void onRestoreInstanceState(Parcelable state)
  {
    SavedState ss = (SavedState) state;

    super.onRestoreInstanceState(ss.getSuperState());

    if (ss.selectedId >= 0)
    {
      dataChanged = true;
      needSync = true;
      syncRowId = ss.selectedId;
      syncPosition = ss.position;
      syncMode = CarouselBaseAdapter.SYNC_SELECTED_POSITION;
      requestLayout();
    }
  }

  @Override
  public SpinnerAdapter getAdapter()
  {
    return adapter;
  }

  @Override
  public void setAdapter(SpinnerAdapter adapter)
  {
    if (null != this.adapter)
    {
      this.adapter.unregisterDataSetObserver(dataSetObserver);
      resetList();
    }

    this.adapter = adapter;
    oldSelectedPosition = CarouselBaseAdapter.INVALID_POSITION;
    oldSelectedRowId = CarouselBaseAdapter.INVALID_ROW_ID;

    if (this.adapter != null)
    {
      oldItemCount = itemCount;
      itemCount = this.adapter.getCount();
      checkFocus();

      dataSetObserver = new AdapterDataSetObserver();
      this.adapter.registerDataSetObserver(dataSetObserver);
      final int position = itemCount > 0 ? 0 : CarouselBaseAdapter.INVALID_POSITION;
      setSelectedPositionInt(position);
      setNextSelectedPositionInt(position);

      if (itemCount == 0)
      {
        // Nothing selected
        checkSelectionChanged();
      }
    }
    else
    {
      checkFocus();
      resetList();
      // Nothing selected
      checkSelectionChanged();
    }

    requestLayout();
    setNextSelectedPositionInt(0);
  }

  @Override
  public View getSelectedView()
  {
    if (itemCount > 0 && selectedPosition >= 0)
    {
      return getChildAt(selectedPosition - firstPosition);
    }

    return null;
  }

  @Override
  public void setSelection(int position)
  {
    setSelectionInt(position, false);
  }

  /**
   * @see android.view.View#measure(int, int)
   * <p/>
   * Figure out the dimensions of this Spinner. The width comes from the widthMeasureSpec as Spinnners can't have their width set to UNSPECIFIED.
   * The height is based on the height of the selected item plus padding.
   */
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
  {
    final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    final int widthSize;
    final int heightSize;

    spinnerPadding.left = getPaddingLeft() > selectionLeftPadding ? getPaddingLeft() : selectionLeftPadding;
    spinnerPadding.top = getPaddingTop() > selectionTopPadding ? getPaddingTop() : selectionTopPadding;
    spinnerPadding.right = getPaddingRight() > selectionRightPadding ? getPaddingRight() : selectionRightPadding;
    spinnerPadding.bottom = getPaddingBottom() > selectionBottomPadding ? getPaddingBottom() : selectionBottomPadding;

    if (dataChanged == true)
    {
      handleDataChanged();
    }

    int preferredHeight = 0;
    int preferredWidth = 0;
    boolean needsMeasuring = true;
    final int selectedPosition = getSelectedItemPosition();

    if (selectedPosition >= 0 && adapter != null && selectedPosition < adapter.getCount())
    {
      // Try looking in the recycler. (Maybe we were measured once already)
      View view = recycler.get(selectedPosition);

      if (view == null)
      {
        // Make a new one
        view = adapter.getView(selectedPosition, null, this);
      }

      if (view != null)
      {
        // Put in recycler for re-measuring and/or layout
        recycler.put(selectedPosition, view);
      }

      if (view != null)
      {
        if (view.getLayoutParams() == null)
        {
          blockLayoutRequests = true;
          view.setLayoutParams(generateDefaultLayoutParams());
          blockLayoutRequests = false;
        }

        measureChild(view, widthMeasureSpec, heightMeasureSpec);

        preferredHeight = getChildHeight(view) + spinnerPadding.top + spinnerPadding.bottom;
        preferredWidth = getChildWidth(view) + spinnerPadding.left + spinnerPadding.right;
        needsMeasuring = false;
      }
    }

    if (needsMeasuring == true)
    {
      // No views -- just use padding
      preferredHeight = spinnerPadding.top + spinnerPadding.bottom;

      if (widthMode == MeasureSpec.UNSPECIFIED)
      {
        preferredWidth = spinnerPadding.left + spinnerPadding.right;
      }
    }

    preferredHeight = Math.max(preferredHeight, getSuggestedMinimumHeight());
    preferredWidth = Math.max(preferredWidth, getSuggestedMinimumWidth());
    heightSize = resolveSize(preferredHeight, heightMeasureSpec);
    widthSize = resolveSize(preferredWidth, widthMeasureSpec);

    setMeasuredDimension(widthSize, heightSize);

    this.heightMeasureSpec = heightMeasureSpec;
    this.widthMeasureSpec = widthMeasureSpec;
  }

  @Override
  protected ViewGroup.LayoutParams generateDefaultLayoutParams()
  {
    /*
     * Carousel expects Carousel.LayoutParams.
     */
    return new Carousel.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
  }

  /**
   * Override to prevent spamming ourselves with layout requests as we place views
   *
   * @see android.view.View#requestLayout()
   */
  @Override
  public void requestLayout()
  {
    if (blockLayoutRequests == false)
    {
      super.requestLayout();
    }
  }

  @Override
  public int getCount()
  {
    return itemCount;
  }

  protected abstract void layout(int delta, boolean animate);

}
