import os
import time
import pandas as pd
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.service import Service
from webdriver_manager.chrome import ChromeDriverManager
from PIL import Image
import io

class ShoppingPriceScraper:
    def __init__(self):
        self.driver = None
        self.screenshots_dir = "screenshots"
        os.makedirs(self.screenshots_dir, exist_ok=True)
    
    def start_driver(self):
        options = webdriver.ChromeOptions()
        options.add_argument("--start-maximized")
        options.add_argument("--disable-extensions")
        options.add_argument("--disable-gpu")
        self.driver = webdriver.Chrome(service=Service(ChromeDriverManager().install()), options=options)
    
    def stop_driver(self):
        if self.driver:
            self.driver.quit()
            self.driver = None
    
    def read_excel(self, input_file):
        df = pd.read_excel(input_file)
        return df['链接'].tolist() if '链接' in df.columns else []
    
    def detect_platform(self, url):
        if 'tmall.com' in url:
            return '天猫'
        elif 'jd.com' in url:
            return '京东'
        elif 'taobao.com' in url:
            return '淘宝'
        else:
            return '未知'
    
    def get_tmall_price(self, url):
        try:
            self.driver.get(url)
            time.sleep(3)
            
            price_element = WebDriverWait(self.driver, 10).until(
                EC.presence_of_element_located((By.CSS_SELECTOR, '.tm-price'))
            )
            price = price_element.text.strip()
            return price
        except Exception as e:
            print(f"获取天猫价格失败: {e}")
            return "获取失败"
    
    def get_jd_price(self, url):
        try:
            self.driver.get(url)
            time.sleep(3)
            
            price_element = WebDriverWait(self.driver, 10).until(
                EC.presence_of_element_located((By.CSS_SELECTOR, '.price.J-p-\d+'))
            )
            price = price_element.text.strip()
            return price
        except Exception as e:
            print(f"获取京东价格失败: {e}")
            return "获取失败"
    
    def get_taobao_price(self, url):
        try:
            self.driver.get(url)
            time.sleep(3)
            
            price_element = WebDriverWait(self.driver, 10).until(
                EC.presence_of_element_located((By.CSS_SELECTOR, '.tb-rmb-num'))
            )
            price = price_element.text.strip()
            return price
        except Exception as e:
            print(f"获取淘宝价格失败: {e}")
            return "获取失败"
    
    def take_screenshot(self, url, index):
        try:
            self.driver.get(url)
            time.sleep(2)
            
            screenshot_path = os.path.join(self.screenshots_dir, f"screenshot_{index}.png")
            self.driver.save_screenshot(screenshot_path)
            return screenshot_path
        except Exception as e:
            print(f"截图失败: {e}")
            return "截图失败"
    
    def handle_anti_robot(self):
        print("\n检测到平台风控，请手动完成验证:")
        print("1. 在弹出的浏览器窗口中完成验证码或登录操作")
        print("2. 完成后按回车键继续...")
        input()
    
    def process_urls(self, urls):
        results = []
        
        for i, url in enumerate(urls):
            print(f"\n处理第 {i+1} 个链接: {url}")
            
            platform = self.detect_platform(url)
            print(f"平台: {platform}")
            
            if platform == '天猫':
                price = self.get_tmall_price(url)
            elif platform == '京东':
                price = self.get_jd_price(url)
            elif platform == '淘宝':
                price = self.get_taobao_price(url)
            else:
                price = "未知平台"
            
            print(f"价格: {price}")
            
            screenshot = self.take_screenshot(url, i)
            print(f"截图: {screenshot}")
            
            results.append({
                '链接': url,
                '平台': platform,
                '价格': price,
                '截图路径': screenshot
            })
            
            if "获取失败" in price:
                print("\n可能遇到风控问题，需要验证...")
                self.handle_anti_robot()
        
        return results
    
    def write_excel(self, results, output_file):
        df = pd.DataFrame(results)
        df.to_excel(output_file, index=False)
        print(f"\n结果已保存到: {output_file}")
    
    def run(self, input_file, output_file):
        print("=== 购物平台价格读取程序 ===")
        print(f"输入文件: {input_file}")
        print(f"输出文件: {output_file}")
        
        self.start_driver()
        
        try:
            urls = self.read_excel(input_file)
            print(f"\n成功读取 {len(urls)} 个链接")
            
            if not urls:
                print("未找到有效链接，请检查Excel文件格式")
                return
            
            results = self.process_urls(urls)
            self.write_excel(results, output_file)
            
            print("\n=== 程序运行完成 ===")
        except Exception as e:
            print(f"程序运行出错: {e}")
        finally:
            self.stop_driver()

if __name__ == "__main__":
    scraper = ShoppingPriceScraper()
    
    input_file = "input.xlsx"
    output_file = "output.xlsx"
    
    print(f"请确保在当前目录下准备好 {input_file} 文件，其中包含'链接'列")
    print("按回车键开始运行...")
    input()
    
    scraper.run(input_file, output_file)