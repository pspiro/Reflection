package test;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;

import tw.google.GTable;
import tw.google.NewSheet;
import tw.util.S;

/** Copy any table from one postgres to another */
public class UpdateEmails {
	static ChromeDriver driver;
	
	public static void main(String[] args) {
		try {
			login();
			S.sleep(1000);
			
			GTable tab = new GTable( NewSheet.Reflection, "LI", "URL", "Email Address");
			
			tab.keySet().forEach( url -> {
				String email = tab.get(url);
				if (S.isNull(email) ) {
					try {
						email = getEmail(url);
						if (S.isNotNull(email) ) {
							tab.put(url, email);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static String getEmail(String urlIn) throws Exception {
		String url = urlIn + "/overlay/contact-info/"; 
		S.out( "Checking " + url);

		// go to one person
		driver.get(url); 
		S.sleep(1000);
		
		String email = driver
				.findElementByXPath("//*[@class=\"artdeco-modal__content ember-view\"]/section/div/section[2]/div/a")
				.getAttribute("href")
				.substring(7);
		
		S.out("  found " + email);
		return email;
	}

	public static ChromeDriver createDriver(String version) throws Exception {
		String exe = String.format( "C:\\temp\\chromedriver.exe"); //chrome-win64\\chrome.exe", version);
		S.out( "Starting chromedriver " + exe);
		System.setProperty(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, exe);
		return new ChromeDriver();
	}
	
	static void login() throws Exception {
		driver = createDriver("116");
		driver.get("https://www.linkedin.com/");
		S.sleep(2000);

		WebElement ele;
		ele = driver.findElementById("session_key");
		ele.sendKeys("peteraspiro@gmail.com");

		ele = driver.findElementById("session_password");
		ele.sendKeys("1359ab");

		// click Login
		ele = driver.findElementByCssSelector("#main-content > section.section.min-h-\\[560px\\].flex-nowrap.pt-\\[40px\\].babybear\\:flex-col.babybear\\:min-h-\\[0\\].babybear\\:px-mobile-container-padding.babybear\\:pt-\\[24px\\] > div > div > form > div.flex.justify-between.sign-in-form__footer--full-width > button");
		ele.click();
		
	}
}
