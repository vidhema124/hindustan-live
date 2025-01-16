package theHindustanLive.entity;

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
	private String pubDate;
	private String publisher;
	private String category;
	private String imageUrl;
	private String publisherIcon;

}
