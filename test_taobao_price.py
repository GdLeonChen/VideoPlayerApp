from main import ShoppingPriceScraper

# 测试读取淘宝链接价格
def test_taobao_price():
    scraper = ShoppingPriceScraper()
    
    # 用户提供的淘宝链接
    url = "https://item.taobao.com/item.htm?id=932503467804&mi_id=0000vaid3qGFgfNqskCs_LzRrChexDWFqsJVshwChDEAJ4g&pvid=a9c2b095-22a7-458f-b4f0-8d393e334601&scm=1007.40986.467924.0&skuId=5954390649953&spm=a21bo.jianhua%2Fa.201876.d13.5af92a89dQmVfC&u_channel=bybtqdyh&umpChannel=bybtqdyh&utparam=%7B%22item_ctr%22%3A0.239658385515213%2C%22x_object_type%22%3A%22item%22%2C%22matchType%22%3A%22dm_interest%22%2C%22item_price%22%3A%228.5%22%2C%22item_cvr%22%3A0.018264252692461014%2C%22umpCalled%22%3Atrue%2C%22pc_ctr%22%3A0.14620667695999146%2C%22pc_scene%22%3A%2220001%22%2C%22userId%22%3A112756163%2C%22ab_info%22%3A%2230986%23467924%230_30986%23528214%2358507_30986%23527805%2358418_30986%23537217%2360408_30986%23521582%2357267_30986%23540637%2361081_30986%23526067%2358189_30986%23524394%2357910_30986%23533297%2359487_30986%23530923%2359037_30986%23532805%2359017_30986%23528109%2358485_30986%23538098%2360586_30986%23538036%2360595%22%2C%22tpp_buckets%22%3A%2230986%23467924%230_30986%23528214%2358507_30986%23527805%2358418_30986%23537217%2360408_30986%23521582%2357267_30986%23540637%2361081_30986%23526067%2358189_30986%23524394%2357910_30986%23533297%2359487_30986%23530923%2359037_30986%23532805%2359017_30986%23528109%2358485_30986%23538098%2360586_30986%23538036%2360595%22%2C%22aplus_abtest%22%3A%22aed8fe33aec4072b5c0c4ec67509b9de%22%2C%22isLogin%22%3Atrue%2C%22abid%22%3A%22528214_527805_537217_521582_540637_526067_524394_533297_530923_532805_528109_538098_538036%22%2C%22pc_pvid%22%3A%22a9c2b095-22a7-458f-b4f0-8d393e334601%22%2C%22isWeekLogin%22%3Afalse%2C%22pc_alg_score%22%3A1.426584383859%2C%22rn%22%3A12%2C%22item_ecpm%22%3A0%2C%22ump_price%22%3A%228.5%22%2C%22isXClose%22%3Afalse%2C%22x_object_id%22%3A932503467804%7D&xxc=home_recommend"
    
    print(f"测试链接: {url}")
    
    # 启动浏览器
    scraper.start_driver()
    
    try:
        # 检测平台
        platform = scraper.detect_platform(url)
        print(f"平台识别: {platform}")
        
        # 获取价格
        if platform == '淘宝':
            price = scraper.get_taobao_price(url)
        elif platform == '天猫':
            price = scraper.get_tmall_price(url)
        elif platform == '京东':
            price = scraper.get_jd_price(url)
        else:
            price = "未知平台"
        
        print(f"获取价格: {price}")
        
        # 截图
        screenshot = scraper.take_screenshot(url, 0)
        print(f"截图保存: {screenshot}")
        
    except Exception as e:
        print(f"测试失败: {e}")
    finally:
        # 关闭浏览器
        scraper.stop_driver()

if __name__ == "__main__":
    test_taobao_price()