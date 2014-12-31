package fr.rolandl.sample.carousel.bo;

import java.io.Serializable;

/**
 * @author Ludovic ROLAND
 * @since 2014.12.20
 */
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
