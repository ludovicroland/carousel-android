package fr.rolandl.sample.carousel.adapter;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import fr.rolandl.carousel.CarouselAdapter;
import fr.rolandl.carousel.CarouselItem;
import fr.rolandl.sample.carousel.R;
import fr.rolandl.sample.carousel.bo.Photo;
import java.util.List;

/**
 * @author Ludovic ROLAND
 * @since 2014.12.20
 */
public final class MyAdapter
    extends CarouselAdapter<Photo>
{

  public static final class PhotoItem
      extends CarouselItem<Photo>
  {

    private ImageView image;

    private TextView name;

    private Context context;

    public PhotoItem(Context context)
    {
      super(context, R.layout.item);
      this.context = context;
    }

    @Override
    public void extractView(View view)
    {
      image = (ImageView) view.findViewById(R.id.image);
      name = (TextView) view.findViewById(R.id.name);
    }

    @Override
    public void update(Photo photo)
    {
      image.setImageResource(getResources().getIdentifier(photo.image, "drawable", context.getPackageName()));
      name.setText(photo.name);
    }

  }

  public MyAdapter(Context context, List<Photo> photos)
  {
    super(context, photos);
  }

  @Override
  public CarouselItem<Photo> getCarouselItem(Context context)
  {
    return new PhotoItem(context);
  }

}
