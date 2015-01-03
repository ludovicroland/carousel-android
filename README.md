# Android 3D Carousel

A simple 3D carousel you can integrate into your Android app.

This version of the library has no additional dependencies, but requires Android
v9+ to work.

Based on the Android 3D Carousel by 
Igor Kushnarev and available on Code Project : http://www.codeproject.com/Articles/146145/Android-D-Carousel

![Sample app screenshot](https://raw.github.com/ludovicroland/carousel-android/master/screenshot1.png)

## Usage

For a full example see the `sample` app in the
[repository](https://github.com/ludovicroland/carousel-android/tree/master/sample).

### From Maven Central

Library releases are available on Maven Central

**Gradle**

```groovy
compile 'fr.rolandl:carousel:1.0.0@aar'
```

**Maven**

```xml
<dependency>
  <groupId>fr.rolandl</groupId>
  <artifactId>carousel</artifactId>
  <version>1.0.0</version>
  <type>aar</type>
</dependency>
```

### As Library Project

Alternatively, check out this repository and add it as a library project.

```console
$ git clone https://github.com/ludovicroland/carousel-android.git
```

Import the project into your favorite IDE and add
`android.library.reference.1=/path/to/carousel-android/library` to your
`project.properties`.

### Layout

You need to declare the `Carousel` directly into your layout.

```xml
<fr.rolandl.carousel.Carousel
  xmlns:android="http://schemas.android.com/apk/res/android"
  android:id="@+id/carousel"
  android:layout_width="match_parent"
  android:layout_height="match_parent"
  android:animationDuration="200"
/>
```

### Items

An item should be associated with a business object (a classical pojo), for example:

```java
public final class Photo
    implements Serializable
{

  private static final long serialVersionUID = 1L;

  public final String name;

  public final String image;

  public Photo(String name, String image)
  {
    this.name = name;
    this.image = image;
  }

}
```

with a specific layout, for example:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
  xmlns:android="http://schemas.android.com/apk/res/android"
  android:orientation="vertical"
  android:layout_width="match_parent"
  android:layout_height="match_parent"
>
  <ImageView
    android:id="@+id/image"
    android:layout_width="100dip"
    android:layout_height="100dip"
    android:scaleType="centerCrop"
  />

  <TextView
    android:id="@+id/name"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:textColor="@android:color/black"
  />
</LinearLayout>
```

and with a CarouselItem that should override the `extractView` and `update` methods :

```java
public final class PhotoItem
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
```

### Adapter

You also have to create your own adapter that takes a list of business object in its constructor:

```java
public final class MyAdapter
    extends CarouselAdapter<Photo>
{

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
```

### In the Activity/Fragment

In the activity or the fragment that uses the carousel, you can find its reference:

```java
final Carousel carousel; = (Carousel) findViewById(R.id.carousel);
```

create your list of business objects:

```java
final List<Photo> photos = new ArrayList<>();
photos.add(new Photo("Photo1", "fotolia_40649376"));
photos.add(new Photo("Photo2", "fotolia_40973414"));
photos.add(new Photo("Photo3", "fotolia_48275073"));
photos.add(new Photo("Photo4", "fotolia_50806609"));
photos.add(new Photo("Photo5", "fotolia_61643329"));
```

create an instance of your adapter:

```java
final CarouselAdapter adapter = adapter = new MyAdapter(this, photos);
carousel.setAdapter(adapter);
adapter.notifyDataSetChanged();
```

### Listeners

You can also use some listeners on the carousel.

The `OnItemClickListener`:

```java
carousel.setOnItemClickListener(new OnItemClickListener()
{

  @Override
  public void onItemClick(CarouselBaseAdapter<?> carouselBaseAdapter, View view, int position, long id)
  {
    Toast.makeText(getApplicationContext(), "The item '" + position + "' has been clicked", Toast.LENGTH_SHORT).show();
    carousel.scrollToChild(position);
  }
  
});
```

The `OnItemLongClickListener`:

```java
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
```

## License

[The Code Project Open License (CPOL) 1.02](http://www.codeproject.com/info/cpol10.aspx)

## Miscellaneous

The photos used into the sample app are from the [Fotolia](http://www.fotolia.com) website.
