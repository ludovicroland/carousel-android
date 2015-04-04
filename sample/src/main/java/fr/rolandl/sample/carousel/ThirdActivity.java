package fr.rolandl.sample.carousel;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import fr.rolandl.sample.carousel.fragment.SecondaryFragment;

/**
 * @author Ludovic ROLAND
 * @since 2015.04.04
 */
public final class ThirdActivity
    extends ActionBarActivity
{

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.third_activity);

    final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
    fragmentTransaction.add(R.id.fragmentContainer, new SecondaryFragment());
    fragmentTransaction.commit();
  }

}
