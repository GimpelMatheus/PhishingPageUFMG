import psutil
import os
from pathlib import Path
import json
from datetime import datetime
import time

def get_configuration(logs_dir_path, threads, page_timeout):
  return {
    'concurrentBrowsers': threads,
    'pageTimeout': page_timeout,
    'logsDirPath': str(logs_dir_path),
    'windowTimeout': 2000,
    'maxRequests': 100,
    'repositoryPath': '../../example/repo',
    'geckodriverBinPath': '/home/mgimpel/geckodriver',
    'runtimeControllersPath': '../runtime_params'
  }


def clear_log_files(log_dir_path):
  to_be_deleted = set(["access_log", "cadeia_urls",
                      "firefox_exception", "http",
                      "http_exception", "source_page",
                      "tcp", "inicio", "null", "time",
                      "requests"])

  for file_name in os.listdir(log_dir_path):
    if(file_name.find('.') >= 0):
      middle_name = file_name.split('.')[1]
      if(middle_name in to_be_deleted):
        # print(log_dir_path.joinpath(file_name))
        
        os.remove(log_dir_path.joinpath(file_name))


def run_framework_and_collect_data(current_workdir, config_file_path, logs_dir_path, runtime_params_dir_path):
  # create log file
  benchmarks_log_file = open(logs_dir_path.joinpath('out.log'), 'w')

  start_time = datetime.now()
  web_phishing_framework = psutil.Popen(['java', '-jar', '../target/WebPhishingFramework.jar', str(config_file_path)],
                                      cwd=str(current_workdir), stdout=benchmarks_log_file, stderr=benchmarks_log_file, text=True)

  cpu_percent = []
  memory_percent = []

  running_time = datetime.now() - start_time
  while(running_time.total_seconds() < (1.5*60*60)): # while the running time is less than two hour and a half
    if(web_phishing_framework.poll() is not None):
      break

    # collect process data
    # write collected data to a file
    with web_phishing_framework.oneshot():
      cpu_percent.append(web_phishing_framework.cpu_percent())
      memory_percent.append(web_phishing_framework.memory_percent(memtype="rss"))

    # sleep for 30 seconds
    time.sleep(30)
    running_time = datetime.now() - start_time

  # Kill the framework
  if(web_phishing_framework.poll() is not None):
    print("========= ERROR ABOVE ========", file=benchmarks_log_file)

  with open(runtime_params_dir_path.joinpath("running"), 'w', encoding='utf-8') as is_running_file:
    print(0, file=is_running_file)
  web_phishing_framework.wait()
  benchmarks_log_file.close()

  # clear files
  clear_log_files(logs_dir_path)

  with open(logs_dir_path.joinpath('cpu_percent.json'), 'w') as cpu_percent_file:
    json.dump(cpu_percent, cpu_percent_file)

  with open(logs_dir_path.joinpath('memory_percent.json'), 'w') as memory_percent_file:
    json.dump(memory_percent, memory_percent_file)


def main():
  # get the path of the current file
  current_filepath = Path(__file__).parent.resolve().absolute()
  runtime_params_dir =  current_filepath.joinpath("runtime_params")
  os.mkdir(runtime_params_dir)

  for concurrent_browsers_number in range(5, 9): # threads number starts from 1 and goes to 8.
    for page_timeout in range(15, 61, 15): # starts from 15 seconds to 60 seconds by incrementing 15 seconds on each iteration.

      # create runtime environment
      with open(runtime_params_dir.joinpath("running"), 'w', encoding='utf-8') as is_running_file:
        print(1, file=is_running_file)

      logs_dir =  current_filepath.joinpath("{}-{}".format(concurrent_browsers_number, page_timeout))
      os.mkdir(logs_dir)

      # write variable parameters
      execution_config = get_configuration(logs_dir, concurrent_browsers_number, page_timeout)
      config_file_path = logs_dir.joinpath('config.json')
      with open(config_file_path, 'w') as json_config_file:
        json.dump(execution_config, json_config_file)


      run_framework_and_collect_data(current_filepath, config_file_path, logs_dir, runtime_params_dir)


if (__name__ == '__main__'):
  main()