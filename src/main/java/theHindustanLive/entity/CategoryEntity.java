package theHindustanLive.entity;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@Document(collection = "category")
public class CategoryEntity {

	private String id;
	private String name;
	private String source;
}
