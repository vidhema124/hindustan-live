package theHindustanLive.serviceimpl;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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
		RSS_URLS.put("https://lankanewsweb.net/archives/category/news/feed/", "Politics");
		RSS_URLS.put("https://www.thehindu.com/sci-tech/science/feeder/default.rss", "Science");
		RSS_URLS.put("https://arynews.tv/category/sci-techno/feed/", "Technology");
		RSS_URLS.put("https://timesofindia.indiatimes.com/rssfeeds/-2128936835.cms", "India");
		RSS_URLS.put("https://timesofindia.indiatimes.com/rssfeeds/3012544.cms", "Local");
		RSS_URLS.put("https://timesofindia.indiatimes.com/rssfeeds/1081479906.cms", "Entertainment");

		PUBLISHER_ICONS.put("The Hindu", "https://vfic.tamu.edu/files/2014/03/LogoThe-Hindu2.png");
		PUBLISHER_ICONS.put("Livemint News",
				"https://tse4.mm.bing.net/th?id=OIP.xMcKwuoYnhP2UBBCt0WdKwHaBv&pid=Api&P=0&h=180");
		PUBLISHER_ICONS.put("Ary News", "https://i.vimeocdn.com/channel/324633_980?mh=250&sig=6ad418f288f7e4319389389a03ebb3a16c0ad00299a5f60a626f9cd2f0c1d736&v=1");
		
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

	                // Check if the news date is today's or yesterday's date
	                if (!isCurrentOrYesterday(pubDate)) {
	                    continue; // Skip news not from today or yesterday
	                }

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

	                // Check if news already exists in the database by title
	                NewsEntity existingNews = mongoTemplate.findOne(Query.query(Criteria.where("title").is(title)), NewsEntity.class);
	                if (existingNews != null) {
	                    // Skip saving the duplicate news
	                    continue;
	                }

	                // Save the new news
	                combinedNewsMap.put(title, news);
	                mongoTemplate.save(news);
	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return new ArrayList<>(combinedNewsMap.values());
	}

	// Helper method to check if the news date is today's or yesterday's date
	private boolean isCurrentOrYesterday(String pubDate) {
	    try {
	        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
	        Date newsDate = dateFormat.parse(pubDate);

	        Calendar calendar = Calendar.getInstance();
	        Date currentDate = calendar.getTime();

	        // Check if the news date is today's or yesterday's date
	        calendar.setTime(currentDate);
	        calendar.add(Calendar.DATE, -1);
	        Date yesterdayDate = calendar.getTime();

	        return !newsDate.before(yesterdayDate) && !newsDate.after(currentDate);
	    } catch (ParseException e) {
	        return false; // If date parsing fails, assume it's not valid
	    }
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
	    } else if (url.contains("arynews")) {
	        return "Ary News";
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
	
	

//	@Override
//	public List<NewsEntity> fetchNewsFromRSS(String categoryFilter) {
//		Map<String, NewsEntity> combinedNewsMap = new HashMap<>();
//
//		try {
//			for (Map.Entry<String, String> entry : RSS_URLS.entrySet()) {
//				String urlString = entry.getKey();
//				String category = entry.getValue();
//
//				if (categoryFilter != null && !category.equalsIgnoreCase(categoryFilter)) {
//					continue;
//				}
//
//				URL url = new URL(urlString);
//				RestTemplate restTemplate = new RestTemplate();
//				String xmlData = restTemplate.getForObject(url.toURI(), String.class);
//
//				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//				DocumentBuilder builder = factory.newDocumentBuilder();
//				InputSource is = new InputSource(new StringReader(xmlData));
//				org.w3c.dom.Document doc = builder.parse(is);
//				NodeList nodeList = doc.getElementsByTagName("item");
//
//				int limit = Math.min(nodeList.getLength(), 10);
//
//				for (int i = 0; i < limit; i++) {
//					Element element = (Element) nodeList.item(i);
//
//					String title = element.getElementsByTagName("title").item(0).getTextContent();
//					String description = element.getElementsByTagName("description").item(0).getTextContent();
//					String link = element.getElementsByTagName("link").item(0).getTextContent();
//					String pubDate = element.getElementsByTagName("pubDate").item(0).getTextContent();
//
//					String imageUrl = extractImageUrl(description);
//					NodeList mediaList = element.getElementsByTagName("media:content");
//					if (mediaList.getLength() > 0) {
//						Element media = (Element) mediaList.item(0);
//						String mediaImageUrl = media.getAttribute("url");
//						if (mediaImageUrl != null && !mediaImageUrl.isEmpty()) {
//							imageUrl = mediaImageUrl;
//						}
//					}
//
//					String publisher = getPublisherFromRSS(urlString);
//					String publisherIcon = getPublisherIcon(publisher);
//					if (publisherIcon == null || !isValidURL(publisherIcon)) {
//						publisherIcon = "https://via.placeholder.com/150?text=No+Logo";
//					}
//
//					NewsEntity news = new NewsEntity();
//					news.setTitle(title);
//					news.setDescription(description);
//					news.setLink(link);
//					news.setPubDate(pubDate);
//					news.setImageUrl(imageUrl);
//					news.setPublisher(publisher);
//					news.setPublisherIcon(publisherIcon);
//					news.setCategory(category);
//
//					if (combinedNewsMap.containsKey(title)) {
//						NewsEntity existingNews = combinedNewsMap.get(title);
//						existingNews.setPublisher(existingNews.getPublisher() + ", " + publisher);
//						existingNews.setPublisherIcon(existingNews.getPublisherIcon() + ", " + publisherIcon);
//					} else {
//						combinedNewsMap.put(title, news);
//						mongoTemplate.save(news);
//					}
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return new ArrayList<>(combinedNewsMap.values());
//	}
//
//	private String extractImageUrl(String description) {
//		if (description == null || description.isEmpty()) {
//			return null;
//		}
//		try {
//			Pattern pattern = Pattern.compile("<img[^>]*src=\"([^\"]*)\"");
//			Matcher matcher = pattern.matcher(description);
//			if (matcher.find()) {
//				return matcher.group(1);
//			}
//		} catch (Exception e) {
//			System.err.println("Error extracting image URL: " + e.getMessage());
//		}
//		return null;
//	}
//
//	private String getPublisherFromRSS(String url) {
//		if (url.contains("thehindu")) {
//			return "The Hindu";
//		} else if (url.contains("indianexpress")) {
//			return "The Indian Express";
//		} else if (url.contains("timesofindia")) {
//			return "Timesofindia";
//		} else if (url.contains("nbcnews")) {
//			return "NBC News";
//		} else if (url.contains("livemint")) {
//			return "Livemint News";
//		}
//		else if (url.contains("arynews")) {
//			return "Ary News";
//		}
//		return "Unknown Publisher";
//	}
//
//	private String getPublisherIcon(String publisher) {
//		return PUBLISHER_ICONS.getOrDefault(publisher, "https://via.placeholder.com/150");
//	}
//
//	private boolean isValidURL(String url) {
//		try {
//			URL validatedUrl = new URL(url);
//			validatedUrl.toURI();
//			return true;
//		} catch (Exception e) {
//			return false;
//		}
//	}

	@Override
	public void saveNews(List<NewsEntity> newsEntities) {
		newRespository.saveAll(newsEntities);
	}

	@Override
	public Map<String, Object> getAllNews(Integer pageNumber, Integer pageSize, String Order) {
	    Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
	    Page<NewsEntity> pageResponse = newRespository.findAll(pageable);
	    List<NewsEntity> newsList = new ArrayList<>(pageResponse.getContent());

	    // Sorting the newsList based on the first letter of the title
	    newsList.sort((news1, news2) -> {
	        String title1 = news1.getTitle();
	        String title2 = news2.getTitle();

	        // Check if sortByFirstLetter is 'none' or any other value
	        if (!"none".equalsIgnoreCase(Order)) {
	            // Get the first letter of the title
	            char firstLetter1 = title1.charAt(0);
	            char firstLetter2 = title2.charAt(0);

	            // Compare first letters
	            int letterComparison = Character.compare(firstLetter1, firstLetter2);

	            // Reverse the order if 'desc' is passed
	            if ("desc".equalsIgnoreCase(Order)) {
	                letterComparison = -letterComparison;  // Reversing the comparison for descending order
	            }

	            // Return the comparison result if different from 0
	            if (letterComparison != 0) {
	                return letterComparison;
	            }
	        }
	        return 0;  // Default behavior (no sorting)
	    });

	    // Preparing the response
	    Map<String, Object> response = new HashMap<>();
	    response.put("news", newsList);
	    response.put("totalPages", pageResponse.getTotalPages());
	    response.put("currentPage", pageResponse.getNumber() + 1);
	    return response;
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
	@Override
	public Map<String, Object> getAllNews(Integer pageNumber, Integer pageSize) {
	    Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
	    Page<NewsEntity> pageResponse = newRespository.findAll(pageable);
	    List<NewsEntity> newsList = new ArrayList<>(pageResponse.getContent());

	   
	    Map<String, Object> response = new HashMap<>();
	    response.put("news", newsList);
	    response.put("totalPages", pageResponse.getTotalPages());
	    response.put("currentPage", pageResponse.getNumber() + 1);
	    return response;
	}

	@Override
	public List<NewsEntity> getAllNews() {
		List<NewsEntity> response=newRespository.findAll();
		return response;
	}

}