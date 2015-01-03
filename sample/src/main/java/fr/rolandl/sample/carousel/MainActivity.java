package fr.rolandl.sample.carousel;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Toast;
import fr.rolandl.carousel.Carousel;
import fr.rolandl.carousel.CarouselAdapter;
import fr.rolandl.carousel.CarouselBaseAdapter;
import fr.rolandl.carousel.CarouselBaseAdapter.OnItemClickListener;
import fr.rolandl.carousel.CarouselBaseAdapter.OnItemLongClickListener;
import fr.rolandl.carousel.CarouselBaseAdapter.OnItemSelectedListener;
import fr.rolandl.sample.carousel.adapter.MyAdapter;
import fr.rolandl.sample.carousel.bo.Photo;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ludovic ROLAND
 * @since 2014.12.19
 */
public final class MainActivity
    extends ActionBarActivity implements OnItemClickListener, OnItemLongClickListener
{

  private CarouselAdapter adapter;

  private Carousel carousel;

  private final List<Photo> photos = new ArrayList<>();

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.main_activity);

    carousel = (Carousel) findViewById(R.id.carousel);

    photos.add(new Photo("Photo1", "fotolia_40649376"));
    photos.add(new Photo("Photo2", "fotolia_40973414"));
    photos.add(new Photo("Photo3", "fotolia_48275073"));
    photos.add(new Photo("Photo4", "fotolia_50806609"));
    photos.add(new Photo("Photo5", "fotolia_61643329"));

    adapter = new MyAdapter(this, photos);
    carousel.setAdapter(adapter);
    adapter.notifyDataSetChanged();

    carousel.setOnItemClickListener(new OnItemClickListener()
    {
      @Override
      public void onItemClick(CarouselBaseAdapter<?> carouselBaseAdapter, View view, int position, long l)
      {
        Toast.makeText(getApplicationContext(), "The item '" + position + "' has been clicked", Toast.LENGTH_SHORT).show();
        carousel.scrollToChild(position);
      }
    });
    carousel.setOnItemLongClickListener(new OnItemLongClickListener()
    {

      @Override
      public boolean onItemLongClick(CarouselBaseAdapter<?> carouselBaseAdapter, View view, int position, long id)
      {
        Toast.makeText(getApplicationContext(), "The item '" + position + "' has been long clicked", Toast.LENGTH_SHORT).show();
        carousel.scrollToChild(position);
        return false;
      }

    });
  }

  @Override
  public void onItemClick(CarouselBaseAdapter<?> parent, View view, int position, long id)
  {
    Toast.makeText(getApplicationContext(), "The item '" + position + "' has been clicked", Toast.LENGTH_SHORT).show();
    carousel.scrollToChild(position);
  }

  @Override
  public boolean onItemLongClick(CarouselBaseAdapter<?> parent, View view, int position, long id)
  {
    Toast.makeText(getApplicationContext(), "The item '" + position + "' has been long clicked", Toast.LENGTH_SHORT).show();
    carousel.scrollToChild(position);
    return false;
  }

}
