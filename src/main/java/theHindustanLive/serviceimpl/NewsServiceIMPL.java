package theHindustanLive.serviceimpl;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import lombok.AllArgsConstructor;
import theHindustanLive.entity.NewsEntity;
import theHindustanLive.respository.NewRespository;
import theHindustanLive.service.NewsService;

@AllArgsConstructor
@Service
public class NewsServiceIMPL implements NewsService {
	NewRespository newRespository;

	@Autowired
	private MongoTemplate mongoTemplate;

	private static final Map<String, String> RSS_URLS = new HashMap<>();
	private static final Map<String, String> PUBLISHER_ICONS = new HashMap<>();

	static {
		RSS_URLS.put("https://www.thehindu.com/sport/feeder/default.rss", "Sports");
		RSS_URLS.put("https://indianexpress.com/feed/", "Sports");
		RSS_URLS.put("https://www.thehindu.com/sci-tech/health/feeder/default.rss", "Health");
		PUBLISHER_ICONS.put("The Hindu",
				"https://vfic.tamu.edu/files/2014/03/LogoThe-Hindu2.png");
		PUBLISHER_ICONS.put("The Indian Express",
				"https://upload.wikimedia.org/wikipedia/commons/thumb/2/24/The_Indian_Express_logo.svg/606px-The_Indian_Express_logo.svg.png");
	}

	@Override
	public List<NewsEntity> fetchNewsFromRSS(String categoryFilter) {
		Map<String, NewsEntity> combinedNewsMap = new HashMap<>();

		try {
			for (Map.Entry<String, String> entry : RSS_URLS.entrySet()) {
				String urlString = entry.getKey();
				String category = entry.getValue();

				if (categoryFilter != null && !category.equalsIgnoreCase(categoryFilter)) {
					continue;
				}

				URL url = new URL(urlString);
				RestTemplate restTemplate = new RestTemplate();
				String xmlData = restTemplate.getForObject(url.toURI(), String.class);

				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				InputSource is = new InputSource();
				is.setCharacterStream(new java.io.StringReader(xmlData));

				org.w3c.dom.Document doc = builder.parse(is);
				NodeList nodeList = doc.getElementsByTagName("item");

				int limit = Math.min(nodeList.getLength(), 10);

				for (int i = 0; i < limit; i++) {
					Element element = (Element) nodeList.item(i);

					String title = element.getElementsByTagName("title").item(0).getTextContent();
					String description = element.getElementsByTagName("description").item(0).getTextContent();
					String link = element.getElementsByTagName("link").item(0).getTextContent();
					String pubDate = element.getElementsByTagName("pubDate").item(0).getTextContent();
					NodeList mediaList = element.getElementsByTagName("media:content");
					String imageUrl = null;

					for (int j = 0; j < mediaList.getLength(); j++) {
						Element media = (Element) mediaList.item(j);
						imageUrl = media.getAttribute("url");
					}

					String publisher = getPublisherFromRSS(urlString);
					String publisherIcon = getPublisherIcon(publisher);

					if (publisherIcon == null || !isValidURL(publisherIcon)) {
						publisherIcon = "https://via.placeholder.com/150?text=No+Logo";
					}

					NewsEntity news = new NewsEntity();

					news.setTitle(title);
					news.setDescription(description);
					news.setLink(link);
					news.setPubDate(pubDate);
					news.setImageUrl(imageUrl);
					news.setPublisher(publisher);
					news.setPublisherIcon(publisherIcon);
					news.setCategory(category);

					if (combinedNewsMap.containsKey(title)) {
						NewsEntity existingNews = combinedNewsMap.get(title);
						existingNews.setPublisher(existingNews.getPublisher() + ", " + publisher);
						existingNews.setPublisherIcon(existingNews.getPublisherIcon() + ", " + publisherIcon);
					} else {
						combinedNewsMap.put(title, news);
						mongoTemplate.save(news);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ArrayList<>(combinedNewsMap.values());
	}

	private String getPublisherFromRSS(String url) {
		if (url.contains("thehindu")) {
			return "The Hindu";
		} else if (url.contains("indianexpress")) {
			return "The Indian Express";
		}
		return "Unknown Publisher";
	}

	private String getPublisherIcon(String publisher) {
		return PUBLISHER_ICONS.getOrDefault(publisher, "https://via.placeholder.com/150");
	}

	private boolean isValidURL(String url) {
		try {
			URL validatedUrl = new URL(url);
			validatedUrl.toURI();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public void saveNews(List<NewsEntity> newsEntities) {
		newRespository.saveAll(newsEntities);
	}

	@Override
	public List<NewsEntity> getAllNews() {
		return newRespository.findAll();
	}

	@Override
	public String deleteNewsById(String id) {

		if (!newRespository.existsById(id)) {
			return "ID does not exist";
		}

		newRespository.deleteById(id);
		return "News deleted successfully";
	}

	public boolean existsById(String id) {
		return newRespository.existsById(id);
	}

	@Override
	public Optional<NewsEntity> newsGetById(String id) {
		Optional<NewsEntity> response = newRespository.findById(id);
		return response;
	}

	@Override
	public Optional<NewsEntity> findById(String id) {
		Optional<NewsEntity> response = newRespository.findById(id);
		return response;
	}

	@Override
	public NewsEntity saveNews(NewsEntity newsEntity) {
		NewsEntity response = newRespository.save(newsEntity);
		return response;

	}

	@Override
	public List<NewsEntity> getRandomNews() {
		return newRespository.findRandomNews();
	}

	@Override
	public List<NewsEntity> getNewsByCategory(String category) {
		return newRespository.findByCategory(category);
	}

	@Override
	public List<Map<String, Object>> getNewsByCategory() {

		List<NewsEntity> allNews = newRespository.findAll();

		Map<String, List<NewsEntity>> groupedNews = allNews.stream()
				.collect(Collectors.groupingBy(NewsEntity::getCategory));

		List<Map<String, Object>> categoryNewsList = new ArrayList<>();
		for (Map.Entry<String, List<NewsEntity>> entry : groupedNews.entrySet()) {
			Map<String, Object> categoryMap = new HashMap<>();
			categoryMap.put("category", entry.getKey());
			categoryMap.put("news", entry.getValue());
			categoryNewsList.add(categoryMap);
		}
		return categoryNewsList;
	}

	@Override
    public List<NewsEntity> getNewsByTitle(String title) {
        String regex = "(?i).*" + title + ".*"; 
        return newRespository.findByTitleRegex(regex);
}
}