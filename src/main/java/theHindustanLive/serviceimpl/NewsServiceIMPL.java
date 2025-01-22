package theHindustanLive.serviceimpl;

import java.net.URL;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
		RSS_URLS.put("https://www.thehindu.com/sci-tech/health/feeder/default.rss", "Health");
		RSS_URLS.put("https://www.thehindu.com/news/international/feeder/default.rss", "World");
		RSS_URLS.put("https://www.thehindu.com/business/feeder/default.rss", "Business");
		RSS_URLS.put("https://feeds.nbcnews.com/nbcnews/public/news", "Politics");
		RSS_URLS.put("https://timesofindia.indiatimes.com/rssfeeds/-2128672765.cms", "Science");
		RSS_URLS.put("https://timesofindia.indiatimes.com/rssfeeds/66949542.cms", "Technology");
		RSS_URLS.put("https://timesofindia.indiatimes.com/rssfeeds/-2128936835.cms", "India");
		RSS_URLS.put("https://timesofindia.indiatimes.com/rssfeeds/3012544.cms", "Local");
		RSS_URLS.put("https://timesofindia.indiatimes.com/rssfeeds/1081479906.cms", "Entertainment");

		PUBLISHER_ICONS.put("The Hindu", "https://vfic.tamu.edu/files/2014/03/LogoThe-Hindu2.png");
		PUBLISHER_ICONS.put("Livemint News",
				"https://tse4.mm.bing.net/th?id=OIP.xMcKwuoYnhP2UBBCt0WdKwHaBv&pid=Api&P=0&h=180");
		PUBLISHER_ICONS.put("NBC News", "https://upload.wikimedia.org/wikipedia/commons/9/97/NBC_News_logo.png");
		PUBLISHER_ICONS.put("Timesofindia", "https://logodix.com/logo/1113831.jpg");
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
				InputSource is = new InputSource(new StringReader(xmlData));
				org.w3c.dom.Document doc = builder.parse(is);
				NodeList nodeList = doc.getElementsByTagName("item");

				int limit = Math.min(nodeList.getLength(), 10);

				for (int i = 0; i < limit; i++) {
					Element element = (Element) nodeList.item(i);

					String title = element.getElementsByTagName("title").item(0).getTextContent();
					String description = element.getElementsByTagName("description").item(0).getTextContent();
					String link = element.getElementsByTagName("link").item(0).getTextContent();
					String pubDate = element.getElementsByTagName("pubDate").item(0).getTextContent();

					String imageUrl = extractImageUrl(description);
					NodeList mediaList = element.getElementsByTagName("media:content");
					if (mediaList.getLength() > 0) {
						Element media = (Element) mediaList.item(0);
						String mediaImageUrl = media.getAttribute("url");
						if (mediaImageUrl != null && !mediaImageUrl.isEmpty()) {
							imageUrl = mediaImageUrl;
						}
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

	private String extractImageUrl(String description) {
		if (description == null || description.isEmpty()) {
			return null;
		}
		try {
			Pattern pattern = Pattern.compile("<img[^>]*src=\"([^\"]*)\"");
			Matcher matcher = pattern.matcher(description);
			if (matcher.find()) {
				return matcher.group(1);
			}
		} catch (Exception e) {
			System.err.println("Error extracting image URL: " + e.getMessage());
		}
		return null;
	}

	private String getPublisherFromRSS(String url) {
		if (url.contains("thehindu")) {
			return "The Hindu";
		} else if (url.contains("indianexpress")) {
			return "The Indian Express";
		} else if (url.contains("timesofindia")) {
			return "Timesofindia";
		} else if (url.contains("nbcnews")) {
			return "NBC News";
		} else if (url.contains("livemint")) {
			return "Livemint News";
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
	public Page<NewsEntity> getAllNews(Integer pageNumber, Integer pageSize) {
	    Pageable page = PageRequest.of(pageNumber - 1, pageSize); 
	    return newRespository.findAllWithImageUrl(page,pageNumber);
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
	public List<NewsEntity> findRandomNewsWithImage() {
		return newRespository.findRandomNewsWithImage();
	}

	@Override
	public List<NewsEntity> getNewsByCategory(String category) {
		return newRespository.findByCategory(category);
	}

	@Override
	public List<Map<String, Object>> getNewsByCategory() {
		List<NewsEntity> allNews = newRespository.findAll();

		List<NewsEntity> filteredNews = allNews.stream()
				.filter(news -> news.getImageUrl() != null && !news.getImageUrl().isEmpty())
				.collect(Collectors.toList());

		Map<String, List<NewsEntity>> groupedNews = filteredNews.stream()
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
	    return newRespository.findByTitleRegexWithImageUrl(regex);  
	}
}