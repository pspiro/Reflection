//package test;
//
//import java.util.Random;
//
//import org.openqa.selenium.By;
//import org.openqa.selenium.WebElement;
//import org.openqa.selenium.chrome.ChromeDriver;
//
//import common.Util;
//import tw.google.GTable;
//import tw.google.NewSheet;
//import tw.util.S;
//
///** Copy any table from one postgres to another */
//public class UpdateEmails {
//	static ChromeDriver driver;
//	
//	public static void main(String[] args) throws Exception {
//
////		login("heather@briscoinvestments.com", "16Sixteen!");
////		S.sleep(1000);
////		process("Heather");
////		driver.close();
//			
//		login("peteraspiro@gmail.com", "1359ab");
//		S.sleep(1000);
//		process("Pete");
//		driver.close();
//	}
//	
//	static void process(String tabName) {
//		try {
//			GTable tab = new GTable( NewSheet.LinkedIn, tabName, "URL", "Email Address");
//			
//			tab.keySet().forEach( url -> {
//				String email = tab.get(url);
//				if (S.isNotNull(url) && S.isNull(email) ) {
//					try {
//						email = getEmail(url);
//						if (S.isNotNull(email) ) {
//							tab.put(url, email);
//						}
//					} catch (Exception e) {
//						e.printStackTrace();
//						//pause();
//					}
//				}
//			});
//		}
//		catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//	
//	static Random rnd = new Random();
//	
//	static String getEmail(String urlIn) throws Exception {
//		String url = urlIn + "/overlay/contact-info/"; 
//		S.out( "Checking " + url);
//
//		// go to one person
//		driver.get(url);
//		S.sleep(20000 + rnd.nextInt(20000) );  // sleep 20-40 seconds
//
//		// //*[@id="ember52"]/section/div/section[2]/div/a
//		String str1 = "/html/body/div[3]/div/div/div[2]/section/div/section[2]/div/a";
//		String str2 = "/html/body/div[3]/div/div/div[2]/section/div/section[3]/div/a";
//		String str3 = "/html/body/div[3]/div/div/div[2]/section/div/section[4]/div/a";
//
//		try {
//			return get(str1);
//		}
//		catch(Exception e) {
//			try {
//				return get(str2);
//			}
//			catch(Exception e2) {
//				return get(str3);
//			}
//		}
//	}
//	
//	static String get(String xpath) {
//		String email = driver
//				.findElement(By.xpath(xpath))
//				.getAttribute("href")
//				.substring(7);
//		
//		S.out("  found " + email);
//		return email;
//	}
//
//	static void login(String username, String pword) throws Exception {
////		ChromeOptions o = new ChromeOptions();
////		o.setBrowserVersion("116");  // if you want to use a different version
//
//		driver = new ChromeDriver(); 
//		driver.get("https://www.linkedin.com/");
//		//S.sleep(1000);
//		Util.pause();
//
//		WebElement ele;
//		ele = driver.findElement(By.id("session_key") );
//		ele.sendKeys(username);
//
//		ele = driver.findElement(By.id("session_password"));
//		ele.sendKeys(pword);
//
//		// click Login
//		ele = driver.findElement(By.cssSelector("#main-content > section.section.min-h-\\[560px\\].flex-nowrap.pt-\\[40px\\].babybear\\:flex-col.babybear\\:min-h-\\[0\\].babybear\\:px-mobile-container-padding.babybear\\:pt-\\[24px\\] > div > div > form > div.flex.justify-between.sign-in-form__footer--full-width > button"));
//		ele.click();
//		
//	}
//}
