print("Python环境测试")
print("当前目录:", __file__)

# 测试基本导入
try:
    import os
    import time
    print("基础模块导入成功")
except Exception as e:
    print(f"基础模块导入失败: {e}")

# 测试Excel相关导入
try:
    import pandas as pd
    print("pandas导入成功")
except Exception as e:
    print(f"pandas导入失败: {e}")

# 测试Selenium导入
try:
    from selenium import webdriver
    print("selenium导入成功")
except Exception as e:
    print(f"selenium导入失败: {e}")