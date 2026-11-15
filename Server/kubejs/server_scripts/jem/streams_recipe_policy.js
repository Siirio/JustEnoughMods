ServerEvents.recipes(event => {
  if (Platform.isLoaded('streamsreflowing')) event.remove({ id: 'streamsreflowing:carve_wand' })
})
