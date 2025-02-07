package theHindustanLive.entity;

import java.time.LocalDateTime;
import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Setter
@Getter
@ToString
@Document(collection = "news")
public class NewsEntity {

	@Id
	private String id;
	private String title;
	private String description;
	private String link;
	private LocalDateTime pubDate;;
	private String publisher;
	private String category;
	private String imageUrl;
	private String publisherIcon;
	private Date updatedAt;
	private Date createdAt;
	private String slug;

	public void generateSlug() {
        if (this.title != null) {
            this.slug = this.title.toLowerCase()
                    .replaceAll("[^a-z0-9\\s]", "") 
                    .replaceAll("\\s+", "-"); 
        }
    }

}
