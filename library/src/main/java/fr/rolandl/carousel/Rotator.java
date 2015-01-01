package fr.rolandl.carousel;

import android.view.animation.AnimationUtils;

/**
 * @author Igor Kushnarev, Ludovic Roland
 * @since 2014.12.19
 * <p/>
 * This class encapsulates rotation. The duration of the rotation can be passed in the constructor and specifies the maximum time that the
 * rotation animation should take. Past this time, the rotation is automatically moved to its final stage and computeRotationOffset() will
 * always return false to indicate that scrolling is over.
 */
//Inspired by http://www.codeproject.com/Articles/146145/Android-D-Carousel
public final class Rotator
{

  private static final int DEFAULT_DURATION = 250;

  private static final float COEFF_VELOCOTY = 0.05f;

  private static final int SCROLL_MODE = 0;

  private static final int FLING_MODE = 1;

  private final float mDeceleration = 240.0f;

  private int mode;

  private float startAngle;

  private float currAngle;

  private long startTime;

  private long duration;

  private float deltaAngle;

  private boolean finished;

  private float velocity;


  /**
   * Create a Scroller with the specified interpolator. If the interpolator is null, the default (viscous) interpolator will be used.
   */
  public Rotator()
  {
    finished = true;
  }

  /**
   * Returns whether the scroller has finished scrolling.
   *
   * @return True if the scroller has finished scrolling, false otherwise.
   */
  public final boolean isFinished()
  {
    return finished;
  }

  /**
   * Force the finished field to a particular value.
   *
   * @param finished The new finished value.
   */
  public final void forceFinished(boolean finished)
  {
    this.finished = finished;
  }

  /**
   * Returns how long the scroll event will take, in milliseconds.
   *
   * @return The duration of the scroll in milliseconds.
   */
  public final long getDuration()
  {
    return duration;
  }

  /**
   * Returns the current X offset in the scroll.
   *
   * @return The new X offset as an absolute distance from the origin.
   */
  public final float getCurrAngle()
  {
    return currAngle;
  }

  /**
   * @return The original velocity less the deceleration. Result may be negative.
   * @hide Returns the current velocity.
   */
  public float getCurrVelocity()
  {
    return Rotator.COEFF_VELOCOTY * velocity - mDeceleration * timePassed() /* / 2000.0f */;
  }

  /**
   * Returns the start X offset in the scroll.
   *
   * @return The start X offset as an absolute distance from the origin.
   */
  public final float getStartAngle()
  {
    return startAngle;
  }

  /**
   * Returns the time elapsed since the beginning of the scrolling.
   *
   * @return The elapsed time in milliseconds.
   */
  public int timePassed()
  {
    return (int) (AnimationUtils.currentAnimationTimeMillis() - startTime);
  }

  public void extendDuration(int extend)
  {
    final int passed = timePassed();
    duration = passed + extend;
    finished = false;
  }

  /**
   * Stops the animation. Contrary to {@link #forceFinished(boolean)}, aborting the animating cause the scroller to move to the final x and y position
   *
   * @see #forceFinished(boolean)
   */
  public void abortAnimation()
  {
    finished = true;
  }

  /**
   * Call this when you want to know the new location. If it returns true, the animation is not yet finished. loc will be altered to provide the new
   * location.
   */
  public boolean computeAngleOffset()
  {
    if (finished == true)
    {
      return false;
    }

    final long systemClock = AnimationUtils.currentAnimationTimeMillis();
    final long timePassed = systemClock - startTime;

    if (timePassed < duration)
    {
      switch (mode)
      {
      case Rotator.SCROLL_MODE:
        final float sc = (float) timePassed / duration;
        currAngle = startAngle + Math.round(deltaAngle * sc);
        break;

      case Rotator.FLING_MODE:
        final float timePassedSeconds = timePassed / 1000.0f;
        float distance;

        if (velocity < 0)
        {
          distance = Rotator.COEFF_VELOCOTY * velocity * timePassedSeconds - (mDeceleration * timePassedSeconds * timePassedSeconds / 2.0f);
        }
        else
        {
          distance = -Rotator.COEFF_VELOCOTY * velocity * timePassedSeconds - (mDeceleration * timePassedSeconds * timePassedSeconds / 2.0f);
        }

        currAngle = startAngle - Math.signum(velocity) * Math.round(distance);
        break;
      }

      return true;
    }
    else
    {
      finished = true;
      return false;
    }
  }

  public void startRotate(float startAngle, float dAngle, int duration)
  {
    mode = Rotator.SCROLL_MODE;
    finished = false;
    this.duration = duration;
    startTime = AnimationUtils.currentAnimationTimeMillis();
    this.startAngle = startAngle;
    deltaAngle = dAngle;
  }

  public void startRotate(float startAngle, float dAngle)
  {
    startRotate(startAngle, dAngle, Rotator.DEFAULT_DURATION);
  }

  /**
   * Start scrolling based on a fling gesture. The distance travelled will depend on the initial velocity of the fling.
   *
   * @param velocityAngle Initial velocity of the fling (X) measured in pixels per second.
   */
  public void fling(float velocityAngle)
  {
    final float velocity = velocityAngle;
    mode = Rotator.FLING_MODE;
    finished = false;
    this.velocity = velocity;
    duration = (int) (1000.0f * Math.sqrt(2.0f * Rotator.COEFF_VELOCOTY * Math.abs(velocity) / mDeceleration));
    startTime = AnimationUtils.currentAnimationTimeMillis();
  }

}
