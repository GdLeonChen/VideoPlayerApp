const { spawn } = require('child_process');
const path = require('path');

console.log('开始构建 Android APK...');

// 设置环境变量
const env = {
  ...process.env,
  JAVA_HOME: 'D:\\Android\\jdk17',
  ANDROID_HOME: 'D:\\Android\\sdk',
  PATH: [
    'D:\\Android\\jdk17\\bin',
    'D:\\Android\\sdk\\platform-tools',
    'D:\\Android\\gradle\\gradle-8.4\\bin',
    process.env.PATH
  ].join(';')
};

const projectDir = 'C:\\Users\\Downloads\\TMALL or JD tag\\VideoPlayerApp';

const gradle = spawn('gradle.bat', ['assembleDebug'], {
  cwd: projectDir,
  env: env,
  shell: true
});

gradle.stdout.on('data', (data) => {
  console.log(data.toString());
});

gradle.stderr.on('data', (data) => {
  console.error(data.toString());
});

gradle.on('close', (code) => {
  console.log(`构建完成，退出码: ${code}`);
  if (code === 0) {
    console.log('APK 输出路径: C:\\Users\\admin\\Downloads\\TMALL or JD tag\\VideoPlayerApp\\app\\build\\outputs\\apk\\debug\\app-debug.apk');
  }
});
