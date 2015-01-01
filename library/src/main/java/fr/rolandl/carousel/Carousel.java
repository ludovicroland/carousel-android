package fr.rolandl.carousel;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Transformation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * @author Igor Kushnarev, Ludovic Roland
 * @since 2014.12.19
 */
//Inspired by http://www.codeproject.com/Articles/146145/Android-D-Carousel
public final class Carousel
    extends CarouselSpinner
    implements GestureDetector.OnGestureListener
{

  private class FlingRotateRunnable
      implements Runnable
  {

    /**
     * Tracks the decay of a fling rotation
     */
    private final Rotator rotator;

    /**
     * Angle value reported by rotator on the previous fling
     */
    private float lastFlingAngle;

    /**
     * Constructor
     */
    public FlingRotateRunnable()
    {
      rotator = new Rotator();
    }

    private void startCommon()
    {
      // Remove any pending flings
      removeCallbacks(this);
    }

    public void startUsingVelocity(float initialVelocity)
    {
      if (initialVelocity == 0)
      {
        return;
      }

      startCommon();
      lastFlingAngle = 0.0f;
      rotator.fling(initialVelocity);
      post(this);
    }

    public void startUsingDistance(float deltaAngle)
    {
      if (deltaAngle == 0)
      {
        return;
      }

      startCommon();

      lastFlingAngle = 0;

      synchronized (this)
      {
        rotator.startRotate(0.0f, -deltaAngle, animationDuration);
      }

      post(this);
    }

    public void stop(boolean scrollIntoSlots)
    {
      removeCallbacks(this);
      endFling(scrollIntoSlots);
    }

    private void endFling(boolean scrollIntoSlots)
    {
      /*
       * Force the scroller's status to finished (without setting its position to the end)
       */
      synchronized (this)
      {
        rotator.forceFinished(true);
      }

      if (scrollIntoSlots)
      {
        scrollIntoSlots();
      }
    }

    @Override
    public void run()
    {
      if (Carousel.this.getChildCount() == 0)
      {
        endFling(true);
        return;
      }

      shouldStopFling = false;

      final Rotator rotator;
      final float angle;
      boolean more;

      synchronized (this)
      {
        rotator = this.rotator;
        more = rotator.computeAngleOffset();
        angle = rotator.getCurrAngle();
      }

      // Flip sign to convert finger direction to list items direction
      // (e.g. finger moving down means list is moving towards the top)
      final float delta = lastFlingAngle - angle;

      // Shoud be reworked
      trackMotionScroll(delta);

      if (more && !shouldStopFling)
      {
        lastFlingAngle = angle;
        post(this);
      }
      else
      {
        lastFlingAngle = 0.0f;
        endFling(true);
      }

    }

  }

  /**
   * Duration in milliseconds from the start of a scroll during which we're unsure whether the user is scrolling or flinging.
   */
  private static final int SCROLL_TO_FLING_UNCERTAINTY_TIMEOUT = 250;

  /**
   * The axe angle
   */
  private static final float THETA = (float) (15.0f * (Math.PI / 180.0));

  /**
   * The info for adapter context menu
   */
  private AdapterContextMenuInfo contextMenuInfo;

  /**
   * How long the transition animation should run when a child view changes position, measured in milliseconds.
   */
  private int animationDuration = 900;

  /**
   * Camera to make 3D rotation
   */
  private final Camera camera = new Camera();

  /**
   * Sets suppressSelectionChanged = false. This is used to set it to false in the future. It will also trigger a selection changed.
   */
  private final Runnable disableSuppressSelectionChangedRunnable = new Runnable()
  {
    @Override
    public void run()
    {
      suppressSelectionChanged = false;
      selectionChanged();
    }
  };

  /**
   * The position of the item that received the user's down touch.
   */
  private int downTouchPosition;

  /**
   * The view of the item that received the user's down touch.
   */
  private View downTouchView;

  /**
   * Executes the delta rotations from a fling or scroll movement.
   */
  private final FlingRotateRunnable flingRunnable = new FlingRotateRunnable();

  /**
   * Helper for detecting touch gestures.
   */
  private final GestureDetector gestureDetector;

  /**
   * Gravity for the widget
   */
  private int gravity;

  /**
   * If true, this onScroll is the first for this user's drag (remember, a drag sends many onScrolls).
   */
  private boolean isFirstScroll;

  /**
   * If true, we have received the "invoke" (center or enter buttons) key down. This is checked before we action on the "invoke" key up, and is
   * subsequently cleared.
   */
  private boolean receivedInvokeKeyDown;

  /**
   * The currently selected item's child.
   */
  private View selectedChild;

  /**
   * Whether to continuously callback on the item selected listener during a fling.
   */
  private boolean shouldCallbackDuringFling = true;

  /**
   * Whether to callback when an item that is not selected is clicked.
   */
  private boolean shouldCallbackOnUnselectedItemClick = true;

  /**
   * When fling runnable runs, it resets this to false. Any method along the path until the end of its run() can set this to true to abort any
   * remaining fling. For example, if we've reached either the leftmost or rightmost item, we will set this to true.
   */
  private boolean shouldStopFling;

  /**
   * If true, do not callback to item selected listener.
   */
  private boolean suppressSelectionChanged;

  public Carousel(Context context)
  {
    this(context, null);
  }

  public Carousel(Context context, AttributeSet attrs)
  {
    this(context, attrs, 0);
  }

  public Carousel(Context context, AttributeSet attrs, int defStyle)
  {
    super(context, attrs, defStyle);

    // It's needed to make items with greater value of
    // z coordinate to be behind items with lesser z-coordinate
    setChildrenDrawingOrderEnabled(true);

    // Making user gestures available
    gestureDetector = new GestureDetector(this.getContext(), this);
    gestureDetector.setIsLongpressEnabled(true);

    // It's needed to apply 3D transforms to items
    // before they are drawn
    setStaticTransformationsEnabled(true);

    // Retrieve settings
    TypedArray arr = getContext().obtainStyledAttributes(attrs, R.styleable.Carousel);
    animationDuration = arr.getInteger(R.styleable.Carousel_android_animationDuration, 400);

    arr.recycle();
  }

  private void Calculate3DPosition(CarouselItem<?> child, int diameter, float angleOffset)
  {
    angleOffset = angleOffset * (float) (Math.PI / 180.0f);

    final float x = -(diameter / 2 * android.util.FloatMath.sin(angleOffset)) + diameter / 2 - child.getWidth() / 2;
    final float z = diameter / 2 * (1.0f - android.util.FloatMath.cos(angleOffset));
    final float y = -getHeight() / 2 + z * android.util.FloatMath.sin(Carousel.THETA);

    child.setItemX(x);
    child.setItemZ(z);
    child.setItemY(y);
  }

  /**
   * Figure out vertical placement based on gravity
   *
   * @param child Child to place
   * @return Where the top of the child should be
   */
  private int calculateTop(View child, boolean duringLayout)
  {
    final int myHeight = duringLayout ? getMeasuredHeight() : getHeight();
    final int childHeight = duringLayout ? child.getMeasuredHeight() : child.getHeight();
    int childTop = 0;

    switch (gravity)
    {
    case Gravity.TOP:
      childTop = spinnerPadding.top;
      break;

    case Gravity.CENTER_VERTICAL:
      int availableSpace = myHeight - spinnerPadding.bottom - spinnerPadding.top - childHeight;
      childTop = spinnerPadding.top + (availableSpace / 2);
      break;

    case Gravity.BOTTOM:
      childTop = myHeight - spinnerPadding.bottom - childHeight;
      break;
    }

    return childTop;
  }

  private boolean dispatchLongPress(View view, int position, long id)
  {
    boolean handled = false;

    if (onItemLongClickListener != null)
    {
      handled = onItemLongClickListener.onItemLongClick(this, downTouchView, downTouchPosition, id);
    }

    if (handled == false)
    {
      contextMenuInfo = new AdapterContextMenuInfo(view, position, id);
      handled = super.showContextMenuForChild(this);
    }

    if (handled == true)
    {
      performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }

    return handled;
  }

  private void dispatchPress(View child)
  {
    if (child != null)
    {
      child.setPressed(true);
    }

    setPressed(true);
  }

  private void dispatchUnpress()
  {
    for (int i = getChildCount() - 1; i >= 0; i--)
    {
      getChildAt(i).setPressed(false);
    }

    setPressed(false);
  }

  /**
   * @return The center of this Gallery.
   */
  private int getCenterOfGallery()
  {
    return (getWidth() - Carousel.this.getPaddingLeft() - Carousel.this.getPaddingRight()) / 2 + Carousel.this.getPaddingLeft();
  }

  /**
   * @return The center of the given view.
   */
  private static int getCenterOfView(View view)
  {
    return view.getLeft() + view.getWidth() / 2;
  }

  private float getLimitedMotionScrollAmount(boolean motionToLeft, float deltaX)
  {
    final int extremeItemPosition = motionToLeft == true ? Carousel.this.getCount() - 1 : 0;
    final View extremeChild = getChildAt(extremeItemPosition - Carousel.this.getFirstVisiblePosition());

    if (extremeChild == null)
    {
      return deltaX;
    }

    final int extremeChildCenter = getCenterOfView(extremeChild);
    final int galleryCenter = getCenterOfGallery();

    if (motionToLeft == true)
    {
      if (extremeChildCenter <= galleryCenter)
      {
        // The extreme child is past his boundary point!
        return 0;
      }
    }
    else
    {
      if (extremeChildCenter >= galleryCenter)
      {
        // The extreme child is past his boundary point!
        return 0;
      }
    }

    final int centerDifference = galleryCenter - extremeChildCenter;
    return motionToLeft == true ? Math.max(centerDifference, deltaX) : Math.min(centerDifference, deltaX);
  }

  private int getLimitedMotionScrollAmount(boolean motionToLeft, int deltaX)
  {
    final int extremeItemPosition = motionToLeft == true ? itemCount - 1 : 0;
    final View extremeChild = getChildAt(extremeItemPosition - firstPosition);

    if (extremeChild == null)
    {
      return deltaX;
    }

    final int extremeChildCenter = getCenterOfView(extremeChild);
    final int galleryCenter = getCenterOfGallery();

    if (motionToLeft == true)
    {
      if (extremeChildCenter <= galleryCenter)
      {
        // The extreme child is past his boundary point!
        return 0;
      }
    }
    else
    {
      if (extremeChildCenter >= galleryCenter)
      {
        // The extreme child is past his boundary point!
        return 0;
      }
    }

    final int centerDifference = galleryCenter - extremeChildCenter;
    return motionToLeft == true ? Math.max(centerDifference, deltaX) : Math.min(centerDifference, deltaX);
  }

  private void makeAndAddView(int position, float angleOffset)
  {
    CarouselItem<?> child;

    if (dataChanged == false)
    {
      child = (CarouselItem<?>) recycler.get(position);

      if (child != null)
      {
        // Position the view
        setUpChild(child, child.getIndex(), angleOffset);
      }
      else
      {
        // Nothing found in the recycler -- ask the adapter for a view
        child = (CarouselItem<?>) adapter.getView(position, null, this);

        // Position the view
        setUpChild(child, child.getIndex(), angleOffset);
      }

      return;
    }

    // Nothing found in the recycler -- ask the adapter for a view
    child = (CarouselItem<?>) adapter.getView(position, null, this);

    // Position the view
    setUpChild(child, child.getIndex(), angleOffset);
  }

  private void onCancel()
  {
    onUp();
  }

  /**
   * Called when rotation is finished
   */
  private void onFinishedMovement()
  {
    if (suppressSelectionChanged == true)
    {
      suppressSelectionChanged = false;

      // We haven't been callbacking during the fling, so do it now
      super.selectionChanged();
    }

    checkSelectionChanged();
    invalidate();
  }

  private void onUp()
  {
    if (flingRunnable.rotator.isFinished() == true)
    {
      scrollIntoSlots();
    }

    dispatchUnpress();
  }

  /**
   * Brings an item with nearest to 0 degrees angle to this angle and sets it selected
   */
  private void scrollIntoSlots()
  {
    // Nothing to do
    if (getChildCount() == 0 || selectedChild == null)
    {
      return;
    }

    // get nearest item to the 0 degrees angle
    // Sort itmes and get nearest angle
    float angle;
    int position;

    ArrayList<CarouselItem<?>> arr = new ArrayList<>();

    for (int i = 0; i < getAdapter().getCount(); i++)
    {
      arr.add(((CarouselItem<?>) getAdapter().getView(i, null, null)));
    }

    Collections.sort(arr, new Comparator<CarouselItem<?>>()
    {

      @Override
      public int compare(CarouselItem<?> c1, CarouselItem<?> c2)
      {
        int a1 = (int) c1.getCurrentAngle();

        if (a1 > 180)
        {
          a1 = 360 - a1;
        }

        int a2 = (int) c2.getCurrentAngle();

        if (a2 > 180)
        {
          a2 = 360 - a2;
        }

        return (a1 - a2);
      }

    });

    angle = arr.get(0).getCurrentAngle();

    // Make it minimum to rotate
    if (angle > 180.0f)
    {
      angle = -(360.0f - angle);
    }

    // Start rotation if needed
    if (angle != 0.0f)
    {
      flingRunnable.startUsingDistance(-angle);
    }
    else
    {
      // Set selected position
      position = arr.get(0).getIndex();
      setSelectedPositionInt(position);
      onFinishedMovement();
    }
  }

  public void scrollToChild(int i)
  {

    final CarouselItem<?> view = (CarouselItem<?>) getAdapter().getView(i, null, null);
    float angle = view.getCurrentAngle();

    if (angle == 0)
    {
      return;
    }

    if (angle > 180.0f)
    {
      angle = 360.0f - angle;
    }
    else
    {
      angle = -angle;
    }

    flingRunnable.startUsingDistance(angle);
  }

  /**
   * Whether or not to callback on any {@link #getOnItemSelectedListener()} while the items are being flinged. If false, only the final selected item
   * will cause the callback. If true, all items between the first and the final will cause callbacks.
   *
   * @param shouldCallback Whether or not to callback on the listener while the items are being flinged.
   */
  public void setCallbackDuringFling(boolean shouldCallback)
  {
    shouldCallbackDuringFling = shouldCallback;
  }

  /**
   * Whether or not to callback when an item that is not selected is clicked. If false, the item will become selected (and re-centered). If true, the
   * {@link #getOnItemClickListener()} will get the callback.
   *
   * @param shouldCallback Whether or not to callback on the listener when a item that is not selected is clicked.
   * @hide
   */
  public void setCallbackOnUnselectedItemClick(boolean shouldCallback)
  {
    shouldCallbackOnUnselectedItemClick = shouldCallback;
  }

  /**
   * Sets how long the transition animation should run when a child view changes position. Only relevant if animation is turned on.
   *
   * @param animationDurationMillis The duration of the transition, in milliseconds.
   * @attr ref android.R.styleable#Gallery_animationDuration
   */
  public void setAnimationDuration(int animationDurationMillis)
  {
    animationDuration = animationDurationMillis;
  }

  public void setGravity(int gravity)
  {
    if (this.gravity != gravity)
    {
      this.gravity = gravity;
      requestLayout();
    }
  }

  private void setUpChild(CarouselItem<?> child, int index, float angleOffset)
  {
    // Ignore any layout parameters for child, use wrap content
    addViewInLayout(child, -1 /* index */, generateDefaultLayoutParams());
    child.setSelected(index == selectedPosition);

    int h;
    int w;
    int d;

    if (isInLayout == true)
    {
      w = child.getMeasuredWidth();
      h = child.getMeasuredHeight();
      d = getMeasuredWidth();

    }
    else
    {
      w = child.getMeasuredWidth();
      h = child.getMeasuredHeight();
      d = getWidth();
    }

    child.setCurrentAngle(angleOffset);

    // Measure child
    child.measure(w, h);

    int childLeft;

    // Position vertically based on gravity setting
    int childTop = calculateTop(child, true);

    childLeft = 0;

    child.layout(childLeft, childTop, w, h);

    Calculate3DPosition(child, d, angleOffset);
  }

  /**
   * Tracks a motion scroll. In reality, this is used to do just about any movement to items (touch scroll, arrow-key scroll, set an item as
   * selected).
   *
   * @param deltaAngle Change in X from the previous event.
   */
  public void trackMotionScroll(float deltaAngle)
  {
    if (getChildCount() == 0)
    {
      return;
    }

    for (int i = 0; i < getAdapter().getCount(); i++)
    {
      CarouselItem<?> child = (CarouselItem<?>) getAdapter().getView(i, null, null);
      float angle = child.getCurrentAngle();
      angle += deltaAngle;

      while (angle > 360.0f)
      {
        angle -= 360.0f;
      }

      while (angle < 0.0f)
      {
        angle += 360.0f;
      }

      child.setCurrentAngle(angle);
      Calculate3DPosition(child, getWidth(), angle);
    }

    // Clear unused views
    recycler.clear();
    invalidate();
  }

  private void updateSelectedItemMetadata()
  {
    final View oldSelectedChild = selectedChild;
    final View child = selectedChild = getChildAt(selectedPosition - firstPosition);

    if (child == null)
    {
      return;
    }

    child.setSelected(true);
    child.setFocusable(true);

    if (hasFocus() == true)
    {
      child.requestFocus();
    }

    // We unfocus the old child down here so the above hasFocus check
    // returns true
    if (oldSelectedChild != null)
    {
      // Make sure its drawable state doesn't contain 'selected'
      oldSelectedChild.setSelected(false);

      // Make sure it is not focusable anymore, since otherwise arrow keys
      // can make this one be focused
      oldSelectedChild.setFocusable(false);
    }
  }

  @Override
  public boolean onDown(MotionEvent e)
  {
    // Kill any existing fling/scroll
    flingRunnable.stop(false);

    // /// Don't know yet what for it is
    // Get the item's view that was touched
    downTouchPosition = pointToPosition((int) e.getX(), (int) e.getY());

    if (downTouchPosition >= 0)
    {
      downTouchView = getChildAt(downTouchPosition - firstPosition);
      downTouchView.setPressed(true);
    }

    // Reset the multiple-scroll tracking state
    isFirstScroll = true;

    // Must return true to get matching events for this down event.
    return true;
  }

  @Override
  public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
  {
    if (shouldCallbackDuringFling == false)
    {
      // We want to suppress selection changes

      // Remove any future code to set suppressSelectionChanged = false
      removeCallbacks(disableSuppressSelectionChangedRunnable);

      // This will get reset once we scroll into slots
      if (suppressSelectionChanged == false)
      {
        suppressSelectionChanged = true;
      }
    }

    // Fling the gallery!
    final int currentSelection = getSelectedItemPosition();
    final int nextSelection;

    if (velocityX > 0)
    {
      nextSelection = currentSelection == getChildCount() - 1 ? 0 : currentSelection + 1;
    }
    else
    {
      nextSelection = currentSelection == 0 ? getChildCount() - 1 : currentSelection - 1;
    }

    final CarouselItem<?> view = (CarouselItem<?>) getAdapter().getView(nextSelection, null, null);
    float angle = view.getCurrentAngle();

    if (angle == 0)
    {
      return false;
    }

    if (angle > 180.0f)
    {
      angle = 360.0f - angle;
    }
    else
    {
      angle = -angle;
    }

    flingRunnable.startUsingDistance(angle);

    return true;
  }

  @Override
  public void onLongPress(MotionEvent e)
  {
    if (downTouchPosition < 0)
    {
      return;
    }

    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    final long id = getItemIdAtPosition(downTouchPosition);
    dispatchLongPress(downTouchView, downTouchPosition, id);
  }

  @Override
  public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
  {
    /*
     * Now's a good time to tell our parent to stop intercepting our events! The user has moved more than the slop amount, since GestureDetector
     * ensures this before calling this method. Also, if a parent is more interested in this touch's events than we are, it would have intercepted
     * them by now (for example, we can assume when a Gallery is in the ListView, a vertical scroll would not end up in this method since a ListView
     * would have intercepted it by now).
     */
    getParent().requestDisallowInterceptTouchEvent(true);

    // As the user scrolls, we want to callback selection changes so related-
    // info on the screen is up-to-date with the gallery's selection
    if (shouldCallbackDuringFling == false)
    {
      if (isFirstScroll == true)
      {
        /*
         * We're not notifying the client of selection changes during the fling, and this scroll could possibly be a fling. Don't do selection changes
         * until we're sure it is not a fling.
         */
        if (suppressSelectionChanged == false)
        {
          suppressSelectionChanged = true;
        }

        postDelayed(disableSuppressSelectionChangedRunnable, Carousel.SCROLL_TO_FLING_UNCERTAINTY_TIMEOUT);
      }
    }
    else
    {
      if (suppressSelectionChanged == true)
      {
        suppressSelectionChanged = false;
      }
    }

    isFirstScroll = false;
    return true;
  }

  @Override
  public boolean onSingleTapUp(MotionEvent e)
  {
    if (downTouchPosition >= 0)
    {
      // Pass the click so the client knows, if it wants to.
      if (shouldCallbackOnUnselectedItemClick == true || downTouchPosition == selectedPosition)
      {
        performItemClick(downTouchView, downTouchPosition, adapter.getItemId(downTouchPosition));
      }

      return true;
    }

    return false;
  }

  // /// Unused gestures
  @Override
  public void onShowPress(MotionEvent e)
  {
  }

  /**
   * Compute the horizontal extent of the horizontal scrollbar's thumb within the horizontal range. This value is used to compute the length of the
   * thumb within the scrollbar's track.
   */
  @Override
  protected int computeHorizontalScrollExtent()
  {
    // Only 1 item is considered to be selected
    return 1;
  }

  /**
   * Compute the horizontal offset of the horizontal scrollbar's thumb within the horizontal range. This value is used to compute the position of the
   * thumb within the scrollbar's track.
   */
  @Override
  protected int computeHorizontalScrollOffset()
  {
    // Current scroll position is the same as the selected position
    return selectedPosition;
  }

  /**
   * Compute the horizontal range that the horizontal scrollbar represents.
   */
  @Override
  protected int computeHorizontalScrollRange()
  {
    // Scroll range is the same as the item count
    return itemCount;
  }

  /**
   * Implemented to handle touch screen motion events.
   */
  @Override
  public boolean onTouchEvent(MotionEvent event)
  {
    // Give everything to the gesture detector
    final boolean retValue = gestureDetector.onTouchEvent(event);
    final int action = event.getAction();

    if (action == MotionEvent.ACTION_UP)
    {
      // Helper method for lifted finger
      onUp();
    }
    else if (action == MotionEvent.ACTION_CANCEL)
    {
      onCancel();
    }

    return retValue;
  }

  /**
   * Extra information about the item for which the context menu should be shown.
   */
  @Override
  protected ContextMenuInfo getContextMenuInfo()
  {
    return contextMenuInfo;
  }

  /**
   * Bring up the context menu for this view.
   */
  @Override
  public boolean showContextMenu()
  {
    if (isPressed() == true && selectedPosition >= 0)
    {
      final int index = selectedPosition - firstPosition;
      final View v = getChildAt(index);

      return dispatchLongPress(v, selectedPosition, selectedRowId);
    }

    return false;
  }

  /**
   * Handles left, right, and clicking
   *
   * @see android.view.View#onKeyDown
   */
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event)
  {
    switch (keyCode)
    {
    case KeyEvent.KEYCODE_DPAD_LEFT:
      playSoundEffect(SoundEffectConstants.NAVIGATION_LEFT);
      return true;

    case KeyEvent.KEYCODE_DPAD_RIGHT:
      playSoundEffect(SoundEffectConstants.NAVIGATION_RIGHT);
      return true;

    case KeyEvent.KEYCODE_DPAD_CENTER:
    case KeyEvent.KEYCODE_ENTER:
      receivedInvokeKeyDown = true;
      // fallthrough to default handling
    }

    return super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event)
  {
    switch (keyCode)
    {
    case KeyEvent.KEYCODE_DPAD_CENTER:
    case KeyEvent.KEYCODE_ENTER:
    {
      if (receivedInvokeKeyDown == true)
      {
        if (itemCount > 0)
        {
          dispatchPress(selectedChild);
          postDelayed(new Runnable()
          {
            @Override
            public void run()
            {
              dispatchUnpress();
            }
          }, ViewConfiguration.getPressedStateDuration());

          final int selectedIndex = selectedPosition - firstPosition;
          performItemClick(getChildAt(selectedIndex), selectedPosition, adapter.getItemId(selectedPosition));
        }
      }

      // Clear the flag
      receivedInvokeKeyDown = false;

      return true;
    }
    }

    return super.onKeyUp(keyCode, event);
  }

  @Override
  protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect)
  {
    super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

    /*
     * The gallery shows focus by focusing the selected item. So, give focus to our selected item instead. We steal keys from our selected item
     * elsewhere.
     */
    if (gainFocus == true && selectedChild != null)
    {
      selectedChild.requestFocus(direction);
    }
  }

  @Override
  protected boolean checkLayoutParams(ViewGroup.LayoutParams p)
  {
    return p instanceof LayoutParams;
  }

  @Override
  protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p)
  {
    return new LayoutParams(p);
  }

  @Override
  public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs)
  {
    return new LayoutParams(getContext(), attrs);
  }

  @Override
  public void dispatchSetSelected(boolean selected)
  {
    /*
     * We don't want to pass the selected state given from its parent to its children since this widget itself has a selected state to give to its
     * children.
     */
  }

  @Override
  protected void dispatchSetPressed(boolean pressed)
  {
    // Show the pressed state on the selected child
    if (selectedChild != null)
    {
      selectedChild.setPressed(pressed);
    }
  }

  @Override
  public boolean showContextMenuForChild(View originalView)
  {
    final int longPressPosition = getPositionForView(originalView);

    if (longPressPosition < 0)
    {
      return false;
    }

    final long longPressId = adapter.getItemId(longPressPosition);

    return dispatchLongPress(originalView, longPressPosition, longPressId);
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event)
  {
    // Gallery steals all key events
    return event.dispatch(this, null, null);
  }

  /**
   * Index of the child to draw for this iteration
   */
  @Override
  protected int getChildDrawingOrder(int childCount, int i)
  {
    // Sort Carousel items by z coordinate in reverse order
    final ArrayList<CarouselItem<?>> sl = new ArrayList<>();

    for (int j = 0; j < childCount; j++)
    {
      final CarouselItem<?> view = (CarouselItem<?>) getAdapter().getView(j, null, null);

      if (i == 0)
      {
        view.setDrawn(false);
      }

      sl.add((CarouselItem<?>) getAdapter().getView(j, null, null));
    }

    Collections.sort(sl);

    // Get first undrawn item in array and get result index
    int idx = 0;

    for (CarouselItem<?> civ : sl)
    {
      if (civ.isDrawn() == false)
      {
        civ.setDrawn(true);
        idx = civ.getIndex();
        break;
      }
    }

    return idx;
  }

  /**
   * Transform an item depending on it's coordinates
   */
  @Override
  protected boolean getChildStaticTransformation(View child, Transformation transformation)
  {
    transformation.clear();
    transformation.setTransformationType(Transformation.TYPE_MATRIX);

    // Center of the view
    final float centerX = (float) getWidth() / 2, centerY = (float) getHeight() / 2;

    // Save camera
    camera.save();

    // Translate the item to it's coordinates
    final Matrix matrix = transformation.getMatrix();

    camera.translate(((CarouselItem<?>) child).getItemX(), ((CarouselItem<?>) child).getItemY(), ((CarouselItem<?>) child).getItemZ());

    // Align the item
    camera.getMatrix(matrix);

    matrix.preTranslate(-centerX, -centerY);
    matrix.postTranslate(centerX, centerY);

    final float[] values = new float[9];
    matrix.getValues(values);

    // Restore camera
    camera.restore();

    final Matrix mm = new Matrix();
    mm.setValues(values);
    ((CarouselItem<?>) child).setCIMatrix(mm);

    // http://code.google.com/p/android/issues/detail?id=35178
    child.invalidate();

    return true;
  }

  /**
   * Setting up images
   */
  @Override
  protected void layout(int delta, boolean animate)
  {
    if (dataChanged == true)
    {
      handleDataChanged();
    }

    // Handle an empty gallery by removing all views.
    if (getCount() == 0)
    {
      resetList();
      return;
    }

    // Update to the new selected position.
    if (nextSelectedPosition >= 0)
    {
      setSelectedPositionInt(nextSelectedPosition);
    }

    // All views go in recycler while we are in layout
    recycleAllViews();

    // Clear out old views
    detachAllViewsFromParent();

    final int count = getAdapter().getCount();
    final float angleUnit = 360.0f / count;
    final float angleOffset = selectedPosition * angleUnit;

    for (int i = 0; i < getAdapter().getCount(); i++)
    {
      float angle = angleUnit * i - angleOffset;

      if (angle < 0.0f)
      {
        angle = 360.0f + angle;
      }
      makeAndAddView(i, angle);
    }

    // Flush any cached views that did not get reused above
    recycler.clear();
    invalidate();
    setNextSelectedPositionInt(selectedPosition);
    checkSelectionChanged();
    needSync = false;
    updateSelectedItemMetadata();
  }

  /**
   * Setting up images after layout changed
   */
  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b)
  {
    super.onLayout(changed, l, t, r, b);

    /*
     * Remember that we are in layout to prevent more layout request from being generated.
     */
    isInLayout = true;
    layout(0, false);
    isInLayout = false;
  }

  @Override
  protected void selectionChanged()
  {
    if (suppressSelectionChanged == false)
    {
      super.selectionChanged();
    }
  }

  @Override
  protected void setSelectedPositionInt(int position)
  {
    super.setSelectedPositionInt(position);
    super.setNextSelectedPositionInt(position);

    // Updates any metadata we keep about the selected item.
    updateSelectedItemMetadata();
  }

}
