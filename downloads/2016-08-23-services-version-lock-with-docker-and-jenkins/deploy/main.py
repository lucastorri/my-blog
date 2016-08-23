#!/usr/bin/env python

import json
import re
from docker import Client
import requests

test_container = '192.168.50.1:5000/e2e-tests:latest'

services = [
  '192.168.50.1:5000/service-a:latest'
]

port_mappings = {
  '192.168.50.1:5000/service-a:latest': { 9000: ('0.0.0.0', 9091) }
}

docker = Client(base_url='unix://var/run/docker.sock')

def latest_versions():
  versions = {}
  for image in docker.images():
    identified_images = set(image['RepoTags']).intersection(services)
    if identified_images:
      versions[identified_images.pop()] = image['Id']
  return versions

def deployed_versions():
  versions = {}
  for container in docker.containers():
    service = container['Image']
    if service in services: versions[service] = container['ImageID']
  return versions

def select_updatable(current_versions, latest_versions):
  updated = []
  for service, latest_version in latest_versions.items():
    if service not in current_versions or current_versions[service] != latest_version:
      updated.append(service)
  return updated

def redeploy(updated_services):
  for service in updated_services:
    name = re.sub(r'[^a-zA-Z0-9]', '_', service)
    print('Will update %s with name %s' % (service, name))
    try:
      docker.remove_container(container=name, force=True)
    except:
      pass
    ports_map = port_mappings.get(service, {})
    host_config = docker.create_host_config(port_bindings=ports_map)
    new_container = docker.create_container(
      image=service,
      name=name,
      ports=list(ports_map.keys()),
      host_config=host_config,
      detach=True)
    docker.start(container=new_container.get('Id'))

def test():
  image_id = docker.history(test_container)[0]['Id']
  tc = docker.create_container(image=test_container, detach=False)
  if docker.start(container=tc.get('Id')) != None:
    raise 'Tests failed'
  if docker.wait(container=tc.get('Id')) != 0:
    raise 'Tests Failed'
  return image_id

def save_versions(versions):
  versions_json = json.dumps(versions, indent=4, sort_keys=True)
  headers = {'content-type': 'application/json'}

  response = requests.post('http://192.168.50.1:8151/', data=versions_json, headers=headers)
  if response.status_code != requests.codes.ok:
    raise 'Invalid response when saving versions'

if __name__ == '__main__':
  new_versions = latest_versions()
  redeploy(select_updatable(deployed_versions(), new_versions))
  test_version = test()
  new_versions.update({ test_container: test_version })
  save_versions(new_versions)
